package librec.ranking;

import java.util.ArrayList;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

public class RLD extends IterativeRecommender{

	private double lambda = 0.4;
	private double beta = 1-lambda;
	private int neg = 30; 
	int[] negS = new int [neg];
	DenseMatrix QC = new DenseMatrix(numItems, numFactors);

	public RLD(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
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

			for (int u = 0; u < numUsers; u++) {
				SparseVector pu = userCache.get(u);

				if (pu.getCount() == 0)
					continue;

				int[] is = pu.getIndex();
				
				if (is.length > 2 || is.length == 2) {
					DenseVector uv = P.row(u);
					for (int x = 0; x < is.length; x++) {
						int i = is[x];
						DenseVector iv = Q.row(i);
						for (int y = 0; y < is.length; y++) {
							if (x == y) continue;
							int k = is[y];
							DenseVector kv = QC.row(k);
							double xuk = uv.inner(kv);
							double xik = iv.inner(kv);
							double val = beta * xuk + lambda * xik; 
							double logval = -Math.log(g(val));
							double sgdval = g(-val);
							loss += logval; 
							for (int f = 0; f < numFactors; f++) {
								double puf = uv.get(f);
								double qif = iv.get(f);
								double qkf = kv.get(f);
								PS.add(u, f, lRate * (sgdval * beta * qkf - regU * puf));
								QS.add(i, f, lRate * (sgdval * lambda * qkf - regI * qif));
								QCS.add(k, f, lRate * (sgdval * (beta * puf + lambda * qif) - regI * qkf));
								loss += regU * puf * puf + regI * qif * qif + regI * qkf * qkf;
							}
							
							//Negative sampling
							for (int N = 0; N < neg; N++) {
								int g = 0;
								do {
									g = Randoms.uniform(numItems);
								} while (pu.contains(g));
								negS[N] = g;
							}
							
							for (int z = 0; z < neg; z++) {
								int g = negS[z];
								DenseVector gv = QC.row(g);
								double xug = uv.inner(gv);
								double xig = iv.inner(gv);
								double vals = beta * xug + lambda * xig;
								double logvals = - Math.log(g(-vals));
								double sgdvals = g(vals);
								loss += logvals;
								for (int f = 0; f < numFactors; f++) {
									double puf = uv.get(f);
									double qif = iv.get(f);
									double qgf = gv.get(f);
									PS.add(u, f, lRate * (sgdvals * (-beta * qgf)));
									QS.add(i, f, lRate * (sgdvals * (-lambda * qgf)));
									QCS.add(g, f, lRate * (sgdvals * (-beta * puf -lambda *  qif) - regI * qgf));
									loss += regI * qgf * qgf;
								}
							}
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

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
	}

}
