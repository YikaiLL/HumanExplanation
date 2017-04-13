package librec.ranking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

public class MP2Vec extends IterativeRecommender{

	private double lambda = 0.1;
	private int neg = 10; 
	int[] negS = new int [neg];
	private Map<Integer, Integer> itemcate = new HashMap<Integer, Integer>(); //save item-category path in the format of ID
	private Map<Integer, ArrayList<Integer>> cilist = new HashMap<Integer, ArrayList<Integer>>();
	private Map<String, Integer> cateID = new HashMap<String, Integer>();
	private DenseMatrix QC = new DenseMatrix(numItems, numFactors);

	public MP2Vec(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		isRankingPred = true;
		initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();
		
		Q.init(0.01);
		QC.init(0.01);

		userCache = trainMatrix.rowCache(cacheSpec);
		System.out.println(numCates);
	}
	
	
	protected void MapToID() {
		int flag = 0;
		for (String item : itempath.keySet()) {
			//System.out.println(item);
			int itemid = rateDao.getItemId(item);
			int size = itempath.get(item).size() - 1;
			if (size < 0) continue;
			String cate = itempath.get(item).get(size);
			if (!cateID.containsKey(cate)) {
				cateID.put(cate, flag);
				itemcate.put(itemid, flag);
				flag++;
			}
			else {
				itemcate.put(itemid, cateID.get(cate));
			}
		}
		numCates = flag;
		
		for (int i : itemcate.keySet()) {
			int cate = itemcate.get(i);
			if (cilist.containsKey(cate)) {
				if (!cilist.get(cate).contains(i)) cilist.get(cate).add(i);
			}
			else{
				ArrayList<Integer> temp = new ArrayList<Integer>();
				temp.add(i);
				cilist.put(cate, temp);
			}
		}
	}
	

	protected void buildModel() throws Exception {
		MapToID();
		
		System.out.println(numCates);
		DenseMatrix C = new DenseMatrix(numCates, numFactors);
		DenseMatrix CC = new DenseMatrix(numCates, numFactors);
		
		//the embedding for the context 
		
		C.init(0.01);
		CC.init(0.01);
		
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		DenseMatrix QCS = new DenseMatrix(numItems, numFactors);
		DenseMatrix CS = new DenseMatrix(numCates, numFactors);
		DenseMatrix CCS = new DenseMatrix(numCates, numFactors);
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;

			for (int i = 0; i < numItems; i++) {
				DenseVector iv = Q.row(i);
				//Save the items that user u rated except item i
				ArrayList<Integer> itemList = new ArrayList<Integer>();
				int[] user = trainMatrix.column(i).getIndex();
				
				//get all the items that co-occur with item i
				for (int x = 0; x < user.length; x++) {
					int u = user[x];
					for (int j : trainMatrix.row(u).getIndex()) {
						if (j == i)  continue;
						itemList.add(j);
					}
				}
				
				//Loss function 1			
				if (itemList.size() == 0) continue; 
				for (int j : itemList) {
					DenseVector jv = QC.row(j);
					double xij = iv.inner(jv);
					double val = -Math.log(g(xij));
					double sgd = g(-xij);
					loss += val;
					for (int f = 0; f < numFactors; f++) {
						double qif = iv.get(f);
						double qjf = jv.get(f);
						QS.add(i, f, lRate * (sgd * qjf - regI * qif));
						QCS.add(j, f, lRate * (sgd * qif - regI * qjf));
						loss += regI * qif * qif + regI * qjf * qjf;
					}
					
					//negative sampling
					int item = 0;
					for (int y = 0; y < neg; y++) {
						do {
							item = Randoms.uniform(numItems);
						} while (itemList.contains(item));
						negS[y] = item;
					}
					
					for (int z = 0; z < negS.length; z++) {
						int k = negS[z];
						DenseVector kv = QC.row(k);
						double xik = iv.inner(kv);
						double vals = - Math.log(g(-xik));
						double sgds = g(xik);
						loss += vals;
						for (int f = 0; f < numFactors; f++) {
							double qif = iv.get(f);
							double qkf = kv.get(f);
							QS.add(i, f, lRate * (sgds * (-qkf)));
							QCS.add(k, f, lRate * (sgds * (-qif) - regI * qkf));
							loss += regI * qkf * qkf;
						}
					}
				}
				
				//Loss function 2
				if (!itemcate.containsKey(i)) continue;
				int mi = itemcate.get(i);
				DenseVector miv = C.row(mi);
				DenseVector icv = QC.row(i);
				for (int x = 0; x < user.length; x++) {
					int u = user[x];
					for (int j : trainMatrix.row(u).getIndex()) {
						if (j == i)  continue;
						else {
							if (!itemcate.containsKey(j)) continue;
							int cate = itemcate.get(j);
							if (cate == mi) {
								double xmii = miv.inner(icv);
								double val = -lambda *  Math.log(g(xmii));
								double sgd = g(-xmii);
								loss += val;
								for (int f = 0; f < numFactors; f++) {
									double mif = miv.get(f);
									double qif = icv.get(f);
									QCS.add(i, f, lRate * (sgd * mif));
									CS.add(mi, f, lRate * (sgd * qif - regI * mif));
									loss += regI * mif * mif;
								}
								
								//negative sampling
								int item = 0;
								for (int y = 0; y < neg; y++) {
									do {
										item = Randoms.uniform(numItems);
									} while (itemList.contains(item));
									negS[y] = item;
								}
								for (int z = 0; z < negS.length; z++) {
									int k = negS[z];
									DenseVector kv = QC.row(k);
									double xmik = miv.inner(kv);
									double vals = -lambda * Math.log(g(-xmik));
									double sgds = g(xmik);
									loss += vals;
									for (int f = 0; f < numFactors; f++) {
										double mif = miv.get(f);
										double qkf = kv.get(f);
										CS.add(mi, f, lRate * (sgds * (-qkf)));
										QCS.add(k, f, lRate * (sgds * (-mif)));
									}
								}
								continue;
							}
						}
					}
				}				
			} 
			
			Q = Q.add(QS);
			QC = QC.add(QCS);
			C = C.add(CS);
			CC = CC.add(CCS);
					
			if (isConverged(iter))
				break;
		}
		
	}

	protected double predict (int u, int i){
		double pred = 0; 
		
		// Take the maximum value as the estimated value 
		for (int k : trainMatrix.row(u).getIndex()) {
			if (k == i) continue;
			double value = DenseMatrix.rowMult(Q, k, Q, i);
			if (pred < value) pred = value;
		}
		return pred;
	}
	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
	}

}
