package librec.ranking;

import java.util.ArrayList;
import java.util.Random;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

public class User2Vec extends IterativeRecommender{

	private int neg = 10; 
	int[] negS = new int [neg];
	DenseMatrix PC = new DenseMatrix(numUsers, numFactors);
	DenseMatrix QC = new DenseMatrix(numItems, numFactors);

	public User2Vec(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
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
		PC.init(0.01);
		QC.init(0.01);

		userCache = trainMatrix.rowCache(cacheSpec);
	}

	protected void buildModel() throws Exception {

		DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		DenseMatrix PCS = new DenseMatrix(numUsers, numFactors);
		DenseMatrix QCS = new DenseMatrix(numItems, numFactors);

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;

			
			for (int u = 0; u < numUsers; u++) {
				SparseVector pu = userCache.get(u);
				if (pu.getCount() == 0)
					continue;

				int[] is = pu.getIndex();
				int size = is.length;
				DenseVector avgi = new DenseVector(numFactors);
				for (int x : is) {
					avgi = avgi.add(QC.row(x));
				}
				
				//update fist loss
				DenseVector uv = PC.row(u);
				for (int x = 0; x < is.length; x++) {
					int i = is[x];
					DenseVector iv = Q.row(i);
					DenseVector kav = avgi.minus(QC.row(i)).add(uv).scale(1.0/size);
					double xkai = kav.inner(iv);
					double val = -Math.log(g(xkai));
					double sgd = g(-xkai);
					loss += val;
					for (int f = 0; f < numFactors; f++){
						double puf = uv.get(f);
						double qif = iv.get(f);
						double qkaf = kav.get(f);
						QS.add(i, f, lRate * (sgd * qkaf - regI * qif));
						PCS.add(u, f, lRate * (sgd * qif / size - regU * puf));
						loss += regI * qif * qif + regU * puf * puf;
					}
					for (int y = 0; y < is.length; y++) {
						if (x == y) continue;
						int k = is[y];
						DenseVector kv = QC.row(k);
						
						//negative sampling
						int item = 0;
						for (int z = 0; z < neg; z++) {
							do {
								item = Randoms.uniform(numItems);
							} while (pu.contains(item));
							negS[z] = item;
						}
						for (int f = 0; f < numFactors; f++) {
							double qif = iv.get(f);
							double qkf = kv.get(f);
							QCS.add(k, f, lRate * (sgd * qif / size - regI * qkf));
							loss += regI * qkf * qkf;
						}
						for (int p : negS) {
							DenseVector pv = Q.row(p);
							double xkap = kav.inner(pv);
							double val2 = -Math.log(g(-xkap));
							double sgd2 = g(xkap);
							loss += val2;
							for (int f = 0; f < numFactors; f++) {
								double qpf = pv.get(f);
								double qkaf = kav.get(f);
								QS.add(p, f, lRate * (sgd2 *(-qkaf)  - regI * qpf));
								QCS.add(k, f, lRate * (sgd2 * (-qpf / size)));
								PCS.add(u, f, lRate * (sgd2 * (-qpf / size)));
								loss += regI * qpf * qpf;
							}
						}				
					}
				}
				
				//update the second loss
				uv = P.row(u);
				DenseVector kav = avgi.scale(1.0/size);
				double xuka = uv.inner(kav);
				double val = -Math.log(g(xuka));
				double sgd = g(-xuka);
				loss += val;
				for (int x = 0; x < is.length; x++) {
					int k = is[x];
					DenseVector kv = QC.row(k);
					for (int f = 0; f < numFactors; f++) {
						double puf = uv.get(f);
						double qkf = kv.get(f);
						double qkaf = kav.get(f);
						PS.add(u, f, lRate * (sgd * qkaf - regU * puf));
						QCS.add(k, f, lRate * (sgd * puf / size));
						loss += regU * puf * puf;
					}
					
					//negative sampling
					int item = 0;
					for (int z = 0; z < neg; z++) {
						do {
							item = Randoms.uniform(numItems);
						} while (pu.contains(item));
						negS[z] = item;
					}
					for (int p : negS) {
						DenseVector pv = QC.row(p);
						double xkap = kav.inner(pv);
						double vals = - Math.log(g(-xkap));
						double sgds = g(xkap);
						loss += vals;
						for (int f = 0; f < numFactors; f++) {
							double qkf = kv.get(f);
							double qpf = pv.get(f);
							double qkaf = kav.get(f);
							QCS.add(k, f, lRate *(sgds * (-qpf / size)));
							QCS.add(p, f, lRate * (sgds * (-qkaf)));
						}
					}
				}				
			}
			
			P = P.add(PS);
			Q = Q.add(QS);
			PC = PC.add(PCS);
			QC = QC.add(QCS);

			if (isConverged(iter))
				break;
		}

	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
	}

}
