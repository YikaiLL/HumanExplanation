package librec.rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

public class VHLF extends IterativeRecommender {

	private double alpha; // the regularization coefficient
	private double begin =0.6;
	private double rate = 0.001; // learning rate of weight
	private double min = 0; 
	private double max = 0.3;
	private double times = 0.6; //the range of random digits [0, times]
	private double regC = regU;
	private Map<Integer, ArrayList<Integer>> hierarchy = new HashMap<Integer, ArrayList<Integer>>(); // save the hierarchy in the format of id
	private Map<String, Integer> cateToID = new HashMap<String, Integer>(); //save the category to id map
	private ArrayList<Integer> layer1 = new ArrayList<Integer>(); // the category set at the first layer of the hierarchy
	private ArrayList<Integer> layer2 = new ArrayList<Integer>(); // the category set at the second layer of the hierarchy
	private ArrayList<Integer> layer3 = new ArrayList<Integer>(); // the category set at the third layer of the hierarchy
	private double[][] weight = new double[numPath][layer]; //parameter vectors for vertical dimension of hierarchy
	private Table<Integer, Integer, Double> icorrs = HashBasedTable.create(); // item co-occurrence index
	private Table<Integer, Integer, Double> ccorrs = HashBasedTable.create(); //feature co-occurrence index

	public VHLF (SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		alpha = cf.getDouble("VHLF.alpha");
		initByNorm = false;
	}

	// Initialize the parameter vectors
	public void initWeight(){

		//initialize the parameter vectors
		for (int i = 0; i < weight.length; i++) {
			Random rand = new Random();
			for (int j = 0; j < layer; j++) {
				double w = rand.nextDouble() * times;
				weight[i][j] = w;
			}			
		}
	}


	//Map string to id for category from down to top
	public void CateToID() {
		int flag = 0;

		ArrayList<String> l1 = new ArrayList<String>();
		ArrayList<String> l2 = new ArrayList<String>();
		ArrayList<String> l3 = new ArrayList<String>();

		//get categories at different layers
		for (String item : itempath.keySet()) {
			String cate1 = itempath.get(item).get(0);
			String cate2 = itempath.get(item).get(1);
			String cate3 = itempath.get(item).get(2);
			if (!l1.contains(cate1)) l1.add(cate1);
			if (!l2.contains(cate2)) l2.add(cate2);
			if (!l3.contains(cate3)) l3.add(cate3);
		}

		//assign id to categories at each layer from down to top

		//layer 1
		for (String cate : l1) {
			if (!cateToID.containsKey(cate)) {
				cateToID.put(cate, flag);
				layer1.add(flag);
				flag++;
			}
		}

		//layer 2
		for (String cate : l2) {
			if (!cateToID.containsKey(cate)) {
				cateToID.put(cate, flag);
				layer2.add(flag);
				flag++;
			}
		}

		//layer 3
		for (String cate : l3) {
			if (!cateToID.containsKey(cate)) {
				cateToID.put(cate, flag);
				layer3.add(flag);
				flag++;
			}
		}		
	}

	//Map item-category path into ID format
	public void PathToID () {
	
		for (String item: itempath.keySet()) {
			ArrayList<Integer> temp = new ArrayList<Integer>();
			int itemid = rateDao.getItemId(item);
			for (String cate: itempath.get(item)) {
				int cateid = cateToID.get(cate);
				temp.add(cateid);
			}
			if (!hierarchy.containsKey(itemid)) hierarchy.put(itemid, temp);
		}
	}
	
	//Map item-item (category-category) table into ID format
	public void MapCorrs() {
	
		for (String i1: itemCorrs.rowKeySet()) {
			int id1 = rateDao.getItemId(i1);
			for (String i2: itemCorrs.row(i1).keySet()) {
				int id2 = rateDao.getItemId(i2);
				//double value = Math.log10(Math.sqrt(itemCorrs.get(i1, i2)));
				double value = Math.log10(itemCorrs.get(i1, i2));
				//double value = itemCorrs.get(i1, i2);
				icorrs.put(id1, id2, value);
			}
		}
		
		for (String c1: cateCorrs.rowKeySet()) {
			int id1 = cateToID.get(c1);
			for (String c2: cateCorrs.row(c1).keySet()) {
				int id2 = cateToID.get(c2);
				double value = cateCorrs.get(c1, c2);
				ccorrs.put(id1, id2, value);
			}
		}
	}


	// Initialize model
	@Override
	protected void initModel() throws Exception{
		super.initModel();

		//initialize the parameter vector
		initWeight();

		//map the hierarchy from string to id
		CateToID();
		PathToID();
		//MapCorrs();
		

		//initialize all the matrices
		P.init(begin);
		Q.init(begin);
		C.init(begin);
	}

	//Calculate the adaptive item vector
	protected DenseVector calAdaIv(int i) {
		DenseVector iv = Q.row(i);
		int pathid = hierarchy.get(i).get(0); // get the id of the parameter vector
		for (int j = 0; j < hierarchy.get(i).size(); j++) {
			int cateid = hierarchy.get(i).get(j);
			//iv = iv.add(C.row(cateid).scale(weight[pathid][j]));
			iv.add(C.row(cateid).scale(weight[pathid][j]));
		}
		return iv;
		
	}

	//Build model
	@Override
	protected void buildModel() throws Exception{
		
		//save last P Q C in case of convergence is achieved before iter = numIters
		DenseMatrix Pold = new DenseMatrix(numUsers, numFactors);
		DenseMatrix Qold = new DenseMatrix(numItems, numFactors);
		DenseMatrix Cold = new DenseMatrix(numCates, numFactors);
		double loss_old = 0;


		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;	

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);
			DenseMatrix CS = new DenseMatrix(numCates, numFactors);

			double[][] WS = new double[numPath][layer];

			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int i = me.column();
				double rui = me.get();

				if (rui <= 0) continue;
				DenseVector uv = P.row(u);
				DenseVector iv = calAdaIv(i);
				double pred = uv.inner(iv);
				double eui = pred - rui;

				loss += eui * eui;

				//update user and item latent vector
				for (int f = 0; f < numFactors; f++) {
					double puf = uv.get(f);
					double qjf = iv.get(f);
					PS.add(u, f, eui * qjf );
					QS.add(i, f, eui * puf );
				}

				//update category latent vector
				int pathid = hierarchy.get(i).get(0);
				for (int x = 0; x < hierarchy.get(i).size(); x++) {

					int cate = hierarchy.get(i).get(x);
					double w = weight[pathid][x];
					DenseVector cv = C.row(cate);
					DenseVector temp = uv.scale(eui * w);

					for (int f = 0; f < numFactors; f++) {
						double tempf = temp.get(f);
						CS.add(cate, f, tempf);
					}

					double tempw = uv.inner(cv) * eui;
					WS[pathid][x] += tempw;
				}

			}

			//Add item regularization terms 
			for (int i : icorrs.rowKeySet()) {
				DenseVector reg = new DenseVector(numFactors);
				System.out.println(reg.get(0));
				DenseVector iv = Q.row(i);
				for (int j : icorrs.row(i).keySet()) {
					Double sigma = icorrs.get(i, j);
					DenseVector jv = Q.row(j);
					reg = reg.add(iv.minus(jv).scale(sigma));
				}
				
				for (int f = 0; f < numFactors; f++) {
					
					double regf = reg.get(f);
					//System.out.println(regf);
					QS.add(i, f, alpha * regf);
					loss += alpha * regf * regf;
				}
			}
			
			// Add category regularization terms
			for (int c1 : ccorrs.rowKeySet()) {

				DenseVector reg = new DenseVector(numFactors);
				DenseVector c1v = C.row(c1);
				for (int c2: ccorrs.row(c1).keySet() ) {
					double sigma = ccorrs.get(c1, c2);
					DenseVector c2v = C.row(c2);
					reg = reg.add(c1v.minus(c2v).scale(sigma));
				}

				for (int f = 0; f < numFactors; f++) {
					double regf = reg.get(f);
					CS.add(c1, f, alpha * regf);
					loss += alpha * regf * regf;
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

			//Add category regularization to avoid over-fitting
			for (int c = 0; c < numCates; c++) {
				for (int f = 0; f < numFactors; f++) {
					double cf = C.get(c, f);
					CS.add(c, f, regC * cf * cf);
					loss += regC * cf * cf;
				}
			}

			//Add parameter vector regularization to avoid over-fitting
			for (int x = 0; x < numPath; x++) {
				for (int y = 0; y < 3; y++) {
					double w = weight[x][y];
					WS[x][y] += regC * w ;
					loss += regC * w * w;
				}
			}


			P = P.add(PS.scale(-lRate));
			Q = Q.add(QS.scale(-lRate));
			C = C.add(CS.scale(-lRate));

			for (int x = 0; x < weight.length; x++) {
				for (int y = 0; y < 3; y++) {
					weight[x][y] = weight[x][y] - rate * WS[x][y];
					if(weight[x][y] > 1.0) weight[x][y] = max;
					if (weight[x][y] < 0) weight[x][y] = min;
					//System.out.println(weight[x][y]);
				}
			}

			loss *= 0.5;
			if (isConverged(iter)) {
				P = Pold;
				Q = Qold;
				C = Cold;
				break;
			}

			Pold = P;
			Qold = Q;
			Cold = C;
			loss_old = loss;
		}
	}

	//Rating prediction
	@Override
	protected double predict (int u, int i, boolean bond) {

		DenseVector iv = calAdaIv(i);
		double pred = P.row(u).inner(iv);
		return pred;
	}
}