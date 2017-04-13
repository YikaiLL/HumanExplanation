package librec.ranking;

import java.util.ArrayList;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

public class Item2Vec extends IterativeRecommender{

	private int neg = 10; 
	int[] negS = new int [neg];
	DenseMatrix QC = new DenseMatrix(numItems, numFactors);

	public Item2Vec(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		isRankingPred = true;
		initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();
		
		//the embedding for the context 
		Q.init(0.1);
		QC.init(0.1);

		userCache = trainMatrix.rowCache(cacheSpec);
	}

	protected void buildModel() throws Exception {
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		DenseMatrix QCS = new DenseMatrix(numItems, numFactors);
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			
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


		//Take the average value as the estimated value
		/*
		int size = trainMatrix.row(u).getIndex().length;
		for (int k : trainMatrix.row(u).getIndex()) {
			if (k == i) continue;
			pred += DenseMatrix.rowMult(Q, k, Q, i);
		}
		if ( size != 0)  pred = pred / size;
		*/
		return pred;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
	}

}
