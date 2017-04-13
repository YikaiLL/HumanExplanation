package librec.rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.DenseVector;
import librec.intf.IterativeRecommender;

public class SVDF extends IterativeRecommender{

	private double regC = regU;
	private Map<Integer, ArrayList<Integer>> hierarchy = new HashMap<Integer, ArrayList<Integer>>(); //save item-category path in the format of ID
	private Map<String, Integer> cateID = new HashMap<String, Integer>(); //save id for each category
	
	public SVDF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		initByNorm = false;
		// TODO Auto-generated constructor stub
	}
	
	//Map item-category path into id format
	protected void MapToID() {
		int flag = 0;

		for (String item : itempath.keySet()) {
			int itemid = rateDao.getItemId(item);
			ArrayList<Integer> temp = new ArrayList<Integer>();
			for (String cate : itempath.get(item)) {
				if (!cateID.containsKey(cate)) {
					cateID.put(cate, flag);
					temp.add(flag);
					flag++;
				}
				else {
					temp.add(cateID.get(cate));
				}
			}
			if (!hierarchy.containsKey(itemid)) hierarchy.put(itemid, temp);
		}
		
		//System.out.println("numCates = "+numCates+";  flag = "+flag--);

	}
	
	@Override
	protected void initModel() throws Exception{
		
		super.initModel();
						
		P.init();
		Q.init();
		C.init();
		
		System.out.println("%%%%% "+numCates);
	}
	
	@Override
	protected void buildModel() throws Exception{
		
		MapToID();
		
		DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		DenseMatrix CS = new DenseMatrix(numCates, numFactors);
		
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;	

			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int i = me.column();
				double rui = me.get();

				if (rui <= 0) continue;
				double pred = predict(u, i, false);
				double eui = pred - rui;

				loss += eui * eui;
				
				DenseVector cv = new DenseVector(numFactors);
				for (int c : hierarchy.get(i)) {
					cv.add(C.row(c));
					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double cf = C.get(c, f);
						CS.add(c, f, (eui * puf /(layer+0.0)));
					}
				}
				
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(i, f);
					double cvf = cv.get(f) / (layer + 3.0);
					PS.add(u, f,( eui * (qjf + cvf)));
					QS.add(i, f,( eui * puf));
				}			
			}
			
			// user regularization
			for (int u : trainMatrix.rows()) {
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					PS.add(u, f, regU * puf);
					loss += regU * puf * puf;
				}
			}
			
			// item regularization
			for (int i : trainMatrix.columns()) {
				for (int f = 0; f < numFactors; f++) {
					double qif = Q.get(i, f);
					QS.add(i, f, regU * qif);
					loss += regI * qif * qif;
				}
			}
			
			// category regularization
			for (int c = 0; c < numCates; c++) {
				for (int f = 0; f < numFactors; f++) {
					double cf = C.get(c, f);
					CS.add(c, f, regC * cf);
					loss += regC * cf * cf;
				}
			}
			
			P = P.add(PS.scale(-lRate));
			Q = Q.add(QS.scale(-lRate));
			C = C.add(CS.scale(-lRate));
			

			loss *= 0.5;
			if (isConverged(iter)) break;
		}
	}
	
	protected double predict (int u, int i, boolean bond) {

		DenseVector uv = P.row(u);
		DenseVector iv = Q.row(i);
		DenseVector cv = new DenseVector(numFactors);
		for (int c : hierarchy.get(i)) {
			cv.add(C.row(c));
		}
			
		double pred = cv.scale(1.0 / layer).add(iv).inner(uv);
		return pred;
	}

}
