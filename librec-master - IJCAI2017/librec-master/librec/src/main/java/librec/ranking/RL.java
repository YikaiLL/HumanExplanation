package librec.ranking;

import java.util.ArrayList;
import java.util.Iterator;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

public class RL extends IterativeRecommender{

	private int neg = 10;
	private double lambda = 1.0;
	int[] negS = new int [neg];
	DenseMatrix QC = new DenseMatrix(numItems, numFactors);

	public RL(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		isRankingPred = true;
		initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();
		
		//the embedding for the context 
		P.init(0.01);
		Q.init(0.01);
		QC.init(0.01);

		userCache = trainMatrix.rowCache(cacheSpec);
	}

	protected void buildModel() throws Exception {
		DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		DenseMatrix QCS = new DenseMatrix(numItems, numFactors);
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			
			//update item2vec
			for (int i = 0; i < numItems; i++) {
				DenseVector iv = Q.row(i);
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
				
				//model update				
				if (itemList.size()<1) continue; 
				for (int k : itemList) {
					DenseVector kv = QC.row(k);
					double xik = iv.inner(kv);
					double val = -Math.log(g(xik));
					double sgd = g(-xik);
					loss += val;
					for (int f = 0; f < numFactors; f++) {
						double qif = iv.get(f);
						double qkf = kv.get(f);
						QS.add(i, f, lRate * (sgd * qkf - regI * qif));
						QCS.add(k, f, lRate * (sgd * qif - regI * qkf));
						loss += regI * qif * qif + regI * qkf * qkf;
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
						int g = negS[z];
						DenseVector gv = QC.row(g);
						double xig = iv.inner(gv);
						double vals = - Math.log(g(-xig));
						double sgds = g(xig);
						loss += vals;
						for (int f = 0; f < numFactors; f++) {
							double qif = iv.get(f);
							double qgf = gv.get(f);
							QS.add(i, f, lRate * (sgds * (-qgf)));
							QCS.add(g, f, lRate * (sgds * (-qif) - regI * qgf));
							loss += regI * qgf * qgf;
						}
					}
				}
			}
			
			//update personalization
			for (int u = 0; u < numUsers; u++) {
				SparseVector pu = userCache.get(u);
				if (pu.getCount() == 0)
					continue;
				DenseVector uv = P.row(u);
				int[] is = pu.getIndex();
				for (int k : is) {
					DenseVector kv = QS.row(k);
					double xuk = uv.inner(kv);
					double val = - Math.log(g(xuk));
					double sgd = g(-xuk);
					loss += lambda * val;
					for (int f = 0; f < numFactors; f++) {
						double puf = uv.get(f);
						double qkf = kv.get(f);
						PS.add(u, f, lRate * (sgd * qkf - regU * puf));
						QCS.add(k, f, lRate * (sgd * puf));
						loss += lambda * regU * puf * puf;
					}
					
					//negative sampling
					int item = 0;
					for (int y = 0; y < neg; y++) {
						do {
							item = Randoms.uniform(numItems);
						} while (pu.contains(item));
						negS[y] = item;
					}
					
					for (int z = 0; z < negS.length; z++) {
						int g = negS[z];
						DenseVector gv = QC.row(g);
						double xug = uv.inner(gv);
						double vals = - Math.log(g(-xug));
						double sgds = g(xug);
						loss += lambda * vals;
						for (int f = 0; f < numFactors; f++) {
							double puf = uv.get(f);
							double qgf = gv.get(f);
							PS.add(u, f, lRate * (sgds * (-qgf)));
							QCS.add(g, f, lRate * (sgds * (-puf)));
						}
					}
				}
			}
			
			P = P.add(PS); 
			Q = Q.add(QS);
			QC = QC.add(QCS);
			
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
		pred = pred + lambda *  DenseMatrix.rowMult(P, u, Q, i);
		return pred;
	}
	
	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
	}

}
