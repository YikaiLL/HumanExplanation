package librec.rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
/**
 * Case 1 (b): one item belongs to multiple categories, and each user and item has the same user and item bias.
 * 
 * **/
public class CMF extends IterativeRecommender{

	private double regC = regU;
	private Table<Integer, Integer, Double> IC = HashBasedTable.create(); //save item-category relationships
	private Map<String, Integer> cateID = new HashMap<String, Integer>(); //save id for each category
	public CMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initModel() throws Exception{
		super.initModel();
		double init = 0.6;
		P.init( );
		Q.init( );
		C.init( );
	}

	//Map item-category path into matrix
	protected void MapToMatrix() {
		int flag = 0;

		for (String item : itempath.keySet()) {
			int itemid = rateDao.getItemId(item);
			for (String cate : itempath.get(item)) {
				if (!cateID.containsKey(cate)) {
					cateID.put(cate, flag);
					IC.put(itemid, flag, 1.0);
					flag++;
				}
				else {

					IC.put(itemid, cateID.get(cate), 1.0);
				}
			}
		}

		//System.out.println("numCates = "+numCates+";  flag = "+flag--);

	}

	protected void buildModel() throws Exception{

		MapToMatrix();

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

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qif = Q.get(i, f);
					PS.add(u, f,( eui * qif));
					QS.add(i, f,( eui * puf));
				}				
			}

			for (int item : IC.rowKeySet()) {
				for (int cate : IC.row(item).keySet()) {
					double ric = predictIC(item, cate);
					double eic = ric - IC.get(item, cate);

					loss += eic * eic;

					for (int f = 0; f < numFactors; f++) {
						double qif = Q.get(item, f);
						double cf = C.get(cate, f);
						QS.add(item, f, eic * cf);
						CS.add(cate, f, eic * qif);
					}
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

			P.add(PS.scale(-lRate));
			Q.add(QS.scale(-lRate));
			C.add(CS.scale(-lRate));

			loss *= 0.5;
			if (isConverged(iter)) break;
		}
	}

	@Override
	protected double predict (int u, int i, boolean bond) {

		double pred = DenseMatrix.rowMult(P, u, Q, i);
		return pred;
	}

	//predict the item-category ratings
	protected double predictIC (int i, int c) {
		double pred = DenseMatrix.rowMult(Q, i, C, c);
		return pred;
	}
}
