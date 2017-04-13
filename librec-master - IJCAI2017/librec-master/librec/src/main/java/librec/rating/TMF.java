package librec.rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

public class TMF extends IterativeRecommender {

	private double rate = 0.00005;
	private double alpha; // the regularization coefficient
	private int continentNum = 0; // the number of the first layer
	private int countryNum = 0; //the number of the second layer
	private int cityNum = 0; // the number of the third layer
	private int group = 0; // the total number of nodes including all leaves
	private int non_leaf;
	private Map<Integer, ArrayList<ArrayList>> node = new HashMap<Integer, ArrayList<ArrayList>>(); // contain all the groups
	private ArrayList<String> continentList = new ArrayList<String>();//save all the continent names;
	private ArrayList<String> countryList = new ArrayList<String>(); //save all the country names
	private ArrayList<String> cityList = new ArrayList<String>(); // save all the city names
	private Map<String, Integer> ContinentMap = new HashMap<String, Integer>(); 
	private Map<String, Integer> CountryMap = new HashMap<String, Integer>(); 
	private Map<String, Integer> CityMap = new HashMap<String, Integer>(); 
	private Map<Integer,String> ConMap = new HashMap<Integer, String>();

	public TMF (SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		alpha = cf.getDouble("TMF.alpha");
	}

	protected void initModel() throws Exception{
		super.initModel();
		double initial = 0.6;
		P.init(initial);
		Q.init(initial);
	}

	//get the continent/country/city list and the seperated number
	protected void getContent() {

		int count = 0; //allcoate id for all the node, including continent, country, and city
		for (String country : continentsinfo.keySet()) {

			String continent = continentsinfo.get(country);
			if (!continentList.contains(continent)) {
				continentList.add(continent);
				count++;
				ContinentMap.put(continent, count);
				ConMap.put(count, continent);
			}	
		}
		System.out.println();
		continentNum = continentList.size();
		System.out.println("continent: "+continentNum);

		for (String user : userinfo.keySet()) {

			for (String country: userinfo.get(user).keySet() ) {

				String city = userinfo.get(user).get(country);

				if (!countryList.contains(country)) {
					countryList.add(country);
				}
				if (!cityList.contains(city)) {
					cityList.add(city);
				}
			}	
		}

		//output the country lists
		for (int i = 0; i < countryList.size(); i++) {
			String country = countryList.get(i);
			count++;
			CountryMap.put(country, count);
			ConMap.put(count, country);
		}

		System.out.println();
		countryNum = countryList.size();
		System.out.println("country: "+countryNum);

		//output the city lists
		for (int j = 0; j < cityList.size(); j++) {
			String city = cityList.get(j);
			count++;
			CityMap.put(city, count);
			ConMap.put(count, city);
		}

		System.out.println();
		cityNum = cityList.size();
		System.out.println("city: "+cityNum);
		//System.out.println(count);
		group = continentNum + countryNum + cityNum;
		non_leaf = continentNum + countryNum + 1;
	}

	//divide users
	protected void divideUser(int cid, int parentid, int element ) {
		if (!node.containsKey(cid)) {
			ArrayList<ArrayList> lists = new ArrayList<ArrayList>();
			ArrayList parent = new ArrayList<>();
			ArrayList child = new ArrayList<>();
			ArrayList temp = new ArrayList<>();
			parent.addAll(node.get(parentid).get(0));
			parent.add(parentid);
			temp.add(element);
			lists.add(parent);
			lists.add(temp);
			lists.add(child);
			node.put(cid, lists);
		}
		else {
			node.get(cid).get(1).add((int) element);
		}
		if (!node.get(parentid).get(2).contains(cid)) {
			node.get(parentid).get(2).add(cid);
		}
	}

	//create the tree structure
	protected void createTree(){

		//root node
		ArrayList<ArrayList> Lists = new ArrayList<ArrayList>();
		ArrayList<Integer> parent = new ArrayList<>();
		ArrayList<Integer> element = new ArrayList<>();
		ArrayList<Integer> child = new ArrayList<>();
		Lists.add(parent);
		Lists.add(element);
		Lists.add(child);
		node.put(0, Lists);

		//first layer - 5 continents	
		for (int uid : trainMatrix.rows()) {
			String user = rateDao.getUserId(uid);
			if (userinfo.containsKey(user)) { 
				for (String country: userinfo.get(user).keySet()) {
					String continent = continentsinfo.get(country);
					int contid = ContinentMap.get(continent);
					divideUser(contid, 0, uid);
				}
			}
		}

		//second layer - 112 countries
		for (int i = 1; i <= continentNum; i++) {
			if (!node.containsKey(i)) continue;
			if (node.get(i).get(1).size() >= 2) {	
				for (Object u : node.get(i).get(1).toArray()) {
					String user = rateDao.getUserId((int)u); //user's raw id
					for (String country : userinfo.get(user).keySet()) {
						int counid = CountryMap.get(country);
						divideUser(counid, i, (int)u);
					}	
				}
			}	
		}

		//third layer - 2873 cities
		for (int i = continentNum+1; i <= continentNum + countryNum; i++) {
			if (!node.containsKey(i)) continue;
			if (node.get(i).get(1).size() >= 2) {
				for (Object u : node.get(i).get(1).toArray()) {
					String user = rateDao.getUserId((int)u);
					for (String country : userinfo.get(user).keySet()) {
						String city = userinfo.get(user).get(country);
						int cityid = CityMap.get(city);
						divideUser(cityid, i, (int)u);
					}
				}
			}
		}
	}	

	//get the coefficent for updating g_p
	protected double getValueG(int id, double[][]coef) {
		double valueg = 1.0;
		for (Object parentid : node.get(id).get(0)) {
			int pid = (int) parentid;
			valueg = valueg * coef[id][1];
		}
		return valueg;
	}

	protected void buildModel() throws Exception{

		getContent();
		createTree();

		//initial g and s; the first column represents g; and the second colunm represents s;
		double[][] coef = new double [non_leaf][2];
		for (int i = 0; i < coef.length; i++) {
			coef[i][0] = 0.5;
			coef[i][1] = 0.5;
		}

		DenseMatrix Pold = new DenseMatrix(numUsers, numFactors);
		DenseMatrix Qold = new DenseMatrix(numItems, numFactors);
		double loss_old = 0;

		for (int iter = 1; iter <= numIters; iter++) {
			System.out.println("...iteration: "+iter);

			loss = 0;	

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);

			double[][] transfer = new double [non_leaf][2];

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
					QS.add(j, f, euj * puf );
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

			//Tree structure regularization

			//First: the users in the same leaf node
			Table<Integer, Integer, Double> L2node = HashBasedTable.create(); //save the L2 norm of users in each country and city
			for (int i = non_leaf; i <= group; i++) {
				if (!node.containsKey(i)) continue;
				Object[] user = node.get(i).get(1).toArray();
				if (user.length >= 2) {
					double value = 0.0;
					for (int j = 0; j < user.length; j++) {
						for (int g = j+1; g < user.length; g++) {
							int userj = (int) user[j];
							int userg = (int) user[g];
							for (int f = 0; f < numFactors; f++) {
								double ejg = P.get(userj, f) - P.get(userg, f);
								PS.add(userj, f, alpha * ejg);
								loss += alpha * ejg * ejg;
								value += ejg * ejg;
							}
						}
					}
					L2node.put(i, i, value);
				}
			}

			//Second: the users in the different leaf node
			for (int i = non_leaf; i <= group; i++) {
				if (!node.containsKey(i)) continue;
				for (int j = i+1; j <= group; j++) {
					if (!node.containsKey(j)) continue;
					ArrayList<Integer> list = new ArrayList<Integer>();
					for (Object pi : node.get(i).get(0)) {
						for (Object pj : node.get(j).get(0)) {
							if (pi == pj) list.add((int)pi);
						}
					}
					double value = 0.0;
					for (Object idi: node.get(i).get(1)) {
						for (Object idj: node.get(j).get(1)) {
							int lastid = list.size() - 1;
							double reg = coef[list.get(lastid)][0];
							for (int num = lastid - 1; num >= 0; num--) {
								reg += coef[list.get(num)][0] + reg * coef[list.get(num)][1];
							}
							int useri = (int) idi;
							int userj = (int) idj;
							for (int f = 0; f < numFactors; f++) {
								double eij = P.get(useri, f) - P.get(userj, f);
								PS.add(useri, f, alpha * reg * eij);
								loss += alpha * reg * eij * eij;
								value += eij * eij;
							}
						}
					}
					L2node.put(i, j, value);
					L2node.put(j, i, value);
				}
			}

			//Update g_p and s_p

			//First update the node of country
			for (int i = continentNum+1; i < non_leaf; i++) {
				if (!node.containsKey(i)) continue;
				double value = getValueG(i, coef);

				double L2g = 0.0;
				Object[] city = node.get(i).get(2).toArray();
				for (int x = 0; x < city.length; x++) {
					int cityx = (int) city[x];
					if (L2node.contains(cityx, cityx)) L2g += L2node.get(cityx, cityx);
					for (int y = x+1; y < city.length; y++) {
						int cityy = (int) city[y];
						if(L2node.contains(cityy, cityy)) L2g += L2node.get(cityx, cityy);
					}
				}
				L2node.put(i, i, L2g);
				transfer[i][0] = L2g * value;
			}

			//Second update the node of continent;
			for (int i = 1; i <= continentNum; i++) {
				if (!node.containsKey(i)) continue;
				double value = getValueG(i, coef);
				double L2g = 0.0;
				Object[] country = node.get(i).get(2).toArray();
				for (int x = 0; x < country.length; x++) {
					int countryx = (int) country[x];
					if(L2node.contains(countryx, countryx)) {L2g += L2node.get(countryx, countryx);}
					for (int y = x+1; y < country.length; y++) {
						int countryy = (int) country[y];
						for (Object cityx : node.get(countryx).get(2)) {
							for (Object cityy : node.get(countryy).get(2)) {
								if(L2node.contains(cityx, cityy)) {L2g += L2node.get((int) cityx, (int) cityy);}
							}
						}
					}
				}
				L2node.put(i, i, L2g);
				transfer[i][0] = L2g * value;
			}

			//Third update the node of root
			double L2g = 0.0;
			for (int i = 1; i <= continentNum; i++) {
				if (!node.containsKey(i)) continue;
				if(L2node.contains(i, i)) L2g += L2node.get(i, i);

			}
			transfer[0][0] = L2g;


			//Final Update
			for (int i = 0; i < coef.length; i++) {
				coef[i][0] = coef[i][0] - rate * Math.sqrt((Math.sqrt(transfer[i][0])) );
				//System.out.print(coef[i][0]);
				if (coef[i][0] < 0) {
					coef[i][0] = 0;
				} 
				else if (coef[i][0] > 1) 
				{
					coef[i][0] = 1;
				}
				coef[i][1] = 1 - coef[i][0];
				int number = 0;
				if(node.containsKey(i)){
					number = node.get(i).get(1).size();
				}
				System.out.println("[fold]"+fold+": "+" Name: "+ConMap.get(i)+"| Number of Users: "+number+"| coefficent: " + ""+coef[i][0]);
				System.out.println(coef[i][1]);
			}

			P = P.add(PS.scale(-lRate));
			Q = Q.add(QS.scale(-lRate));

			loss *= 0.5;
			if (isConverged(iter)) {
				P = Pold;
				Q = Qold;
				loss = loss_old;
				break;
			}

			Pold = P;
			Qold = Q;
			loss_old = loss;
		}
	}

	protected double predict (int u, int j, boolean bond) {

		double pred = DenseMatrix.rowMult(P, u, Q, j);
		return pred;
	}
}