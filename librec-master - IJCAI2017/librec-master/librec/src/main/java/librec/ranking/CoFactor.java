package librec.ranking;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;
import librec.util.FileIO;
import librec.util.Randoms;
import librec.util.Strings;

public class CoFactor extends IterativeRecommender{

	private int neg = 20;
	private int number = 13;
	DenseMatrix QC = new DenseMatrix(numItems, numFactors);
	Table<Integer, Integer, Double> PMI = HashBasedTable.create();
	DenseVector tBias = new DenseVector(numItems); 
	DenseVector cBias = new DenseVector(numItems); 

	public CoFactor(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
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
		
		// initialize target and context bias
		tBias.init(initMean, initStd);
		cBias.init(initMean, initStd);
	}
	
	//compute the number of ratings each item obtained
	protected void NumRate() {
		int total = 0;
		for (int i = 0; i < numItems; i++) {
			int size = trainMatrix.column(i).getIndex().length;
			if (size > number) total++;
		}
		System.out.println(total);
	}
	
	//Get all the items
	protected void buildPMI() throws Exception {
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		for (int i = 0; i < numItems; i++) {
			int numI = trainMatrix.column(i).getIndex().length;
			if (numI == 0) continue;
			int total = 0;
			int[] userI = trainMatrix.column(i).getIndex();
			for (int j = i+1; j < numItems; j++) {
				int numJ = trainMatrix.column(j).getIndex().length;
				if (numJ == 0) continue;
				for (int u : userI) {
					if (trainMatrix.row(u).contains(j)) {
						total++;
					}
				}
				if (total == 0) continue;
				double value = Math.log(total * numItems / numI * numJ);
				double xvk = value - Math.log(neg);
				//System.out.println(xvk);
				if (xvk > 0) {
					PMI.put(i, j, xvk);
				}
				//System.out.println(total);
			}
		}	
	}
	
	//Sample some items
	protected void buildPMIS() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		int item = 0;
		for (int i = 0; i < numItems; i++) {
			int size = trainMatrix.column(i).getIndex().length;
			if (size > number) list.add(i);
		}
		
		int size = list.size();
		for (int i = 0; i < list.size(); i++) {
			int numI = trainMatrix.column(i).getIndex().length;
			if (numI == 0) continue;
			int total = 0;
			int[] userI = trainMatrix.column(i).getIndex();
			for (int j = i+1; j < list.size(); j++) {
				int numJ = trainMatrix.column(j).getIndex().length;
				if (numJ == 0) continue;
				for (int u : userI) {
					if (trainMatrix.row(u).contains(j)) {
						total++;
					}
				}
				if (total == 0) continue;
				double value = Math.log(total * size / numI * numJ);
				double xvk = value - Math.log(neg);
				//System.out.println(xvk);
				if (xvk > 0) {
					PMI.put(i, j, xvk);
					PMI.put(j, i, xvk);
				}
				//System.out.println(total);
			}
		}	
	}
	
	protected void outPut() {
		String PMIPath = "D:/experiments/Datasets/datasets-IJCAI2017/clothing/PMI.txt";
		try {
			FileIO.deleteFile(PMIPath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String> lines = new ArrayList<>();
		for (int i : PMI.rowKeySet()) {
			for (int j : PMI.row(i).keySet()) {
				double value = PMI.get(i, j);
				String iid = rateDao.getItemId(i);
				String jid = rateDao.getItemId(j);
				String line = iid + " "+jid +" "+value;
				lines.add(line);
			}
		}

		try {
			FileIO.writeList(PMIPath, lines, null, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void buildModel() throws Exception {
		
		
		//NumRate();
		
		buildPMIS();
		
		DenseMatrix PS = new DenseMatrix(numItems, numFactors);
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		DenseMatrix QCS = new DenseMatrix(numItems, numFactors);
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			for (MatrixEntry me : trainMatrix) {

				int u = me.row(); // user
				int i = me.column(); // item
				double rui = me.get();

				double pui = predict(u, i, false);
				double euj = rui - pui;

				loss += euj * euj;

				// update factors
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qif = Q.get(i, f);
					PS.add(u, f, lRate * (euj * qif - regU * puf));
					QS.add(i, f, lRate * (euj * puf - regI * qif));
					loss += regU * puf * puf + regI * qif * qif;
				}
			}
			
			for (int i : PMI.rowKeySet()) {
				for (int j : PMI.row(i).keySet()) {
					double mij = PMI.get(i, j);
					double pij = Predictm(i, j);
					double eij = mij - pij;
					loss += eij * eij;
					
					double ti = tBias.get(i);
					tBias.add(i, lRate * eij);
					
					double cj = tBias.get(j);
					cBias.add(j, lRate * eij);
					
					// update factors
					for (int f = 0; f < numFactors; f++) {
						double qif = Q.get(i, f);
						double qjf = QC.get(j, f);
						QS.add(i, f, lRate * (eij * qjf));
						QCS.add(j, f, lRate * (eij * qif - regI * qjf));
						loss +=  regI * qjf * qjf;
					}
				}
			}
			
			P.add(PS);
			Q.add(QS);
			QC.add(QCS);
			if (isConverged(iter))
				break;
		}
		
	}
	
	protected double Predictm(int i, int j) {
		
		double pred = DenseMatrix.rowMult(Q, i, QC, j) + tBias.get(i) + cBias.get(j);
		return pred;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, numIters }, ",");
	}

}
