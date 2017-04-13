package librec.ranking;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

public class PR extends IterativeRecommender{
	
	private int neg = 1;
	
	public PR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		isRankingPred = true;
		//Matrix initialized with a random value between 0 and 1;
		initByNorm = false; 
	}
	
	@Override
	protected void initModel() throws Exception {
		super.initModel();
		P.init(0.01);
		Q.init(0.01);
				
		userCache = trainMatrix.rowCache(cacheSpec);
	}
	
	protected void buildModel() throws Exception{
		
		DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			for (int s = 0, smax = numUsers * 100; s < smax; s++) {

				// randomly draw (u, i, j)
				int u = 0, i = 0, j = 0, g = 0, h = 0;
				int[][] negS = new int [neg][2];

				while (true) {
					u = Randoms.uniform(numUsers);
					SparseVector pu = userCache.get(u);
					
					if (pu.getCount() == 0)
						continue;

					int[] is = pu.getIndex();
					i = is[Randoms.uniform(is.length)];

					do {
						j = Randoms.uniform(numItems);
					} while (pu.contains(j));
					
					//negative sampling
					for (int N = 0; N < neg; N++) {
						do {
							g = Randoms.uniform(numItems);
							h = Randoms.uniform(numItems);
						} while ((g == h) || pu.contains(g) || pu.contains(h));
						negS[N][0] = g;
						negS[N][1] = h;
					}
					break;
				}

				// update parameters
				double xui = predict(u, i);
				double xuj = predict(u, j);
				double xuij = xui - xuj;

				double vals = -Math.log(g(xuij));
				loss += vals;

				double cmg = g(-xuij);
				
				DenseVector uv = P.row(u);
				DenseVector iv = Q.row(i);
				DenseVector jv = Q.row(j);

				for (int f = 0; f < numFactors; f++) {
					double puf = uv.get(f);
					double qif = iv.get(f);
					double qjf = jv.get(f);

					PS.add(u, f, lRate * (cmg * (qif - qjf) - regU * puf));
					QS.add(i, f, lRate * (cmg * puf - regI * qif));
					QS.add(j, f, lRate * (cmg * (-puf) - regI * qjf));

					loss += regU * puf * puf + regI * qif * qif + regI * qjf * qjf;
				}
				
				for (int k = 0; k < negS.length; k++) {
					int gid = negS[k][0]; 
					int hid = negS[k][1];
					double xug = predict(u, gid);
					double xuh = predict(u, hid);
					double xugh = xug - xuh; 
					double valneg = -Math.log(g(xugh));
					loss += valneg; 
					
					double cmgneg = g(-xugh);
					for (int f = 0; f < numFactors; f++) {
						double puf = uv.get(f);						
						double qgf = Q.get(g, f);
						double qhf = Q.get(h, f);
						
						PS.add(u, f, lRate * cmgneg * (qgf - qhf));
						QS.add(g, f, lRate * (puf - regI * qgf));
						QS.add(h, f, lRate * (-puf - regI * qhf));
						
						loss += regI * qgf * qgf + regI * qhf * qhf;
					}
				}
			}
			
			P = P.add(PS);
			Q = Q.add(QS);

			if (isConverged(iter))
				break;

		}
	}
	
	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
	}
}
