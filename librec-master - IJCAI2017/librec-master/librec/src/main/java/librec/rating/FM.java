package librec.rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.DenseVector;
import librec.intf.IterativeRecommender;

public class FM extends IterativeRecommender{

	private double regC = regU;
	private Map<Integer, ArrayList<Integer>> hierarchy = new HashMap<Integer, ArrayList<Integer>>(); //save item-category path in the format of ID
	private Map<String, Integer> cateID = new HashMap<String, Integer>(); //save id for each category
	
	public FM(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
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
	}
	
	@Override
	protected void initModel() throws Exception{
		
		super.initModel();
		
		double value = 0.6;
						
		P.init(value);
		Q.init(value);
		C.init(value);
		
		userBias = new DenseVector(numUsers);
		itemBias = new DenseVector(numItems);
		cateBias = new DenseVector(numCates);

		// initialize user bias
		userBias.init(initMean, initStd);
		itemBias.init(initMean, initStd);
		cateBias.init(initMean, initStd);
		
		//userBias.init( );
		//itemBias.init( );
		//cateBias.init( );
		
	}
	
	@Override
	protected void buildModel() throws Exception{
		
		MapToID();
		
		DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		DenseMatrix CS = new DenseMatrix(numCates, numFactors);
		DenseVector UB = new DenseVector(numUsers);
		DenseVector IB = new DenseVector(numItems);
		DenseVector CB = new DenseVector(numCates);
		
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;	

			for (MatrixEntry me : trainMatrix) {
				int u = me.row(); // user
				int i = me.column(); // item
				double rui = me.get();

				double pred = predict(u, i, false);
				double eui = pred - rui;

				loss += eui * eui;

				// update factors
				UB.add(u,  eui);
				IB.add(i,  eui);
				
				DenseVector uv = P.row(u);
				DenseVector iv = Q.row(i);
				DenseVector temp = new DenseVector(numFactors);
				
				for (int c : hierarchy.get(i)) {
					CB.add(c, eui);
					DenseVector cv = C.row(c);
					temp.add(cv);
					DenseVector sgdcv = uv.add(iv).scale(eui);
					for (int f = 0; f < numFactors; f++) {
						double sgdcvf = sgdcv.get(f);
						CS.add(c, f, sgdcvf);
					}					
				}
				
				DenseVector sgduv = temp.add(iv).scale(eui);
				DenseVector sgdiv = temp.add(uv).scale(eui);
				
				for (int f = 0; f < numFactors; f++) {
					double sgduvf = sgduv.get(f);
					double sgdivf = sgdiv.get(f);
					PS.add(u, f, sgduvf);
					QS.add(i, f, sgdivf);
				}			
			}
			
			//user regularization
			for (int u: trainMatrix.rows()) {
				
				double bu = userBias.get(u);
				UB.add(u, regB * bu);
				loss += regB * bu * bu;
				
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					PS.add(u, f, regU * puf);
					loss += regU * puf * puf;
				}
			}
			
			//item regularization
			for (int i : trainMatrix.columns()) {
				
				double bi = itemBias.get(i);
				IB.add(i, regB * bi);
				loss += regB * bi * bi;
				
				for (int f = 0; f < numFactors; f++) {
					double qif = Q.get(i, f);
					QS.add(i, f, regI * qif);
					loss += regI * qif * qif;
				}
			}
			
			//category regularization
			for (int c = 0; c < numCates; c++) {
				
				double bc = cateBias.get(c);
				CB.add(c,  regB * bc);
				loss += regB * bc * bc;
				
				for (int f = 0; f < numFactors; f++) {
					double cf = C.get(c, f);
					CS.add(c, f, regC * cf);
					loss += regC * cf * cf;
				}
			}
			
			P = P.add(PS.scale(-lRate));
			Q = Q.add(QS.scale(-lRate));
			C = C.add(CS.scale(-lRate));
			userBias.add(UB.scale(-lRate));
			itemBias.add(IB.scale(-lRate));
			cateBias.add(CB.scale(-lRate));
			
			
			loss *= 0.5;
			if (isConverged(iter)) break;
		}
	}
	
	protected double predict (int u, int i, boolean bond) {

		double pred = globalMean + userBias.get(u) + itemBias.get(i);
		DenseVector uv = P.row(u);
		DenseVector iv = Q.row(i);
		DenseVector temp = new DenseVector(numFactors);
		for (int c : hierarchy.get(i)) {
			DenseVector cv = C.row(c);
			pred += uv.inner(cv) + iv.inner(cv)+ cateBias.get(c);
		}		
		pred += uv.inner(iv);
		//System.out.println(pred);
		return pred;
	}

}
