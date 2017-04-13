package librec.main;

import librec.util.FileIO;

public class IR {
	
	static int alg = 3;

	public static void main(String[] args) throws Exception {
		String dirPath = FileIO.makeDirPath("demo");
		String configDirPath = FileIO.makeDirPath(dirPath, "config");

		String configFile = "IR.conf";
		
		
		if (alg == 1) {
			configFile = "IR.conf";
		} else if (alg == 2) {
			configFile = "IRTMF.conf";
		} else if (alg == 3) {
			configFile = "BPR.conf";
		}
		
		LibRec librec = new LibRec();
		librec.setConfigFiles(configDirPath + configFile);
		librec.execute(args);
	}

}
