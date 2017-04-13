package librec.ranking;

import java.util.ArrayList;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;

public class RPR extends IterativeRecommender{
	
	private int negE = 20;
	private int negP = 10;
	private double lambda = 0.4;
	private double beta = 1 - lambda;
	int[] negEm = new int [negE];
	int[][] negPe = new int [negP][2];
	DenseMatrix QC = new DenseMatrix(numItems, numFactors);
	
	public RPR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		isRankingPred = true;
		initByNorm = false;
	}
	
	@Override
	protected void initModel() throws Exception {
		super.initModel();
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
							for (int N = 0; N < negE; N++) {
								int g = 0;
								do {
									g = Randoms.uniform(numItems);
								} while (pu.contains(g));
								negEm[N] = g;
							}
							
							for (int z = 0; z < negE; z++) {
								int g = negEm[z];
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
			
			for (int s = 0, smax = numUsers * 100; s < smax; s++) {
				// randomly draw (u, i, j)
				int u = 0, i = 0, j = 0, g = 0, h = 0;
				

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
					for (int N = 0; N < negP; N++) {
						do {
							g = Randoms.uniform(numItems);
							h = Randoms.uniform(numItems);
						} while ((g == h) || pu.contains(g) || pu.contains(h));
						negPe[N][0] = g;
						negPe[N][1] = h;
					}
					break;
				}

				// update parameters
				DenseVector uv = P.row(u);
				DenseVector iv = QC.row(i);
				DenseVector jv = QC.row(j);
				double xui = uv.inner(iv);
				double xuj = uv.inner(jv);
				double xuij = xui - xuj;

				double vals = -Math.log(g(xuij));
				loss += vals;

				double cmg = g(-xuij);

				for (int f = 0; f < numFactors; f++) {
					double puf = uv.get(f);
					double qif = iv.get(f);
					double qjf = jv.get(f);

					PS.add(u, f, lRate * (cmg * (qif - qjf)));
					QCS.add(i, f, lRate * (cmg * puf));
					QCS.add(j, f, lRate * (cmg * (-puf) - regI * qjf));

					//loss += regU * puf * puf + regI * qif * qif + regI * qjf * qjf;
					loss += regI * qjf * qjf;
				}
				
				for (int k = 0; k < negPe.length; k++) {
					g = negPe[k][0]; 
					h = negPe[k][1];
					DenseVector gv = QC.row(g);
					DenseVector hv = QC.row(h);
					double xug = uv.inner(gv);
					double xuh = uv.inner(hv);
					double xugh = xug - xuh; 
					double valneg = -Math.log(g(xugh));
					loss += valneg; 
					
					double cmgneg = g(-xugh);
					for (int f = 0; f < numFactors; f++) {
						double puf = uv.get(f);						
						double qgf = gv.get(f);
						double qhf = hv.get(f);
						
						PS.add(u, f, lRate * cmgneg * (qgf - qhf));
						QCS.add(g, f, lRate * (puf));
						QCS.add(h, f, lRate * (-puf));
						
						//loss += regI * qgf * qgf + regI * qhf * qhf;
					}
				}
			}
			
			P = P.add(PS);
			Q = Q.add(QS);
			QC =QC.add(QCS);
					
			if (isConverged(iter))
				break;
		}
	}
	
}
