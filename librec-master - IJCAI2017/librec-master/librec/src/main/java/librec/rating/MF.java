package librec.rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
/**
 * Case 1 (b): one item belongs to multiple categories, and each user and item has the same user and item bias.
 * 
 * **/
public class MF extends IterativeRecommender{

	private Multiset<Integer> cateDist = HashMultiset.create();
	public MF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		// TODO Auto-generated constructor stub
	}
	
	protected void intialModel() throws Exception{
		super.initModel();
	}
	
	protected void buildModel() throws Exception{
		
		
		DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
		DenseMatrix QS = new DenseMatrix(numItems, numFactors);
		
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;	

			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				if (ruj <= 0) continue;
				double pred = predict(u, j, false);
				double euj = pred - ruj;

				loss += euj * euj;

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);
					PS.add(u, f, euj * qjf);
					QS.add(j, f, euj * puf);
				}
			}
			
			//user regularization
			for (int u : trainMatrix.rows()) {
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					PS.add(u, f, regU * puf);
					loss += regU * puf * puf;
				}
			}
			
			//item regularization
			for (int j : trainMatrix.columns()) {
				for (int f = 0; f < numFactors; f++) {
					double qjf = Q.get(j, f);
					QS.add(j, f, regI * qjf);
					loss += regI * qjf * qjf;
				}
			}
			
			P.add(PS.scale(-lRate));
			Q.add(QS.scale(-lRate));

			loss *= 0.5;
			if (isConverged(iter)) break;
		}
	}
	
	protected double predict (int u, int j, boolean bond) {

		double pred = DenseMatrix.rowMult(P, u, Q, j);
		return pred;
	}

}
