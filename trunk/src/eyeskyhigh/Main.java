package eyeskyhigh;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eyeskyhigh.api.SearchTask;
import eyeskyhigh.lucene.SearchEngineWrapper;

public class Main {

	private static ThreadPoolExecutor tpe;
	private static SearchEngineWrapper lucene;
	
	public static void queueFile(String path) {
		File fin = new File(path);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new BufferedInputStream(new FileInputStream(fin))));
			String line = null;
			while((line = br.readLine()) != null) {
				final String tLine = line;
				tpe.execute(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							lucene.indexFile(new File(tLine));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please give index file location and list of indexing locations");
            System.exit(1);
        }
        lucene = new SearchEngineWrapper(args[0]);
        try {
			lucene.initialize();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//init the thread pool executor
		tpe = new ThreadPoolExecutor(1, 3, 50L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
		
        try {
            for(int i = 1; i < args.length; i++) {
                
                    lucene.indexFile(new File(args[i]));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        BufferedReader in;
		try {
			in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return;
		}
        char opt = '0';
        long starttime = 0;
        while(opt != '3') {
        	System.out.println("1. Index files");
        	System.out.println("2. Search files");
        	System.out.println("3. Exit");
        	try {
				opt = in.readLine().charAt(0);
			} catch (IOException e) {
				continue;
			}
        	switch(opt) {
        	case '1':
        		try {
        			starttime = System.currentTimeMillis();
        			queueFile(in.readLine());
        			opt = '3';
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				break;
        	case '2':
//        		try {
//					lucene.searchFiles(new SearchTask(in.readLine(), "", "contents"));
//				} catch (IOException e) {
//					e.printStackTrace();
//					continue;
//				}
				break;
        	}
        }
        tpe.shutdown();
        try {
			tpe.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        lucene.dispose();
        System.out.println("Index time: " + (System.currentTimeMillis() - starttime));
        System.out.println("Completed " + tpe.getCompletedTaskCount() + " tasks");
    }

}
