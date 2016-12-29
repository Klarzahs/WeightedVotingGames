import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import util.BanzhafThread;
import util.Log;
import util.PowerSet;

public class Main extends ApplicationFrame{
	// ------ Weighted Voting Games vars ----
	private static final int MAXPLAYER = 15;
	private static final int MINPLAYER = 5;
	private static final int MEAN = 200;
	private static final int VARIANCE = 30;
	private static final int C = 50;
	
	private double[] averageReducing = new double[100];
	private int[] numberOfMinimums = new int[100];
	
	private final JFreeChart chart;

	/*
	 * Idea: calculate the current banzhaf index for a random target. Then
	 * create C variations with quota qi = (qb + i * C/d) Iterate over these new
	 * banzhaf indexes and get the lowest one. If it is below the starting
	 * index, we achieved a lowering of the power of the target player. Do that
	 * x times for each of the MAXQUOTACHANGE (q') values (for example: 100x 20%
	 * quota lowering) and calculate the mean possible lowering for this q'
	 * value. Then increase the allowed lowering and restart
	 */
	
	public Main(final String title) {
	    super(title);
	    final XYSeries series = new XYSeries("Quota Reduction");
		calculate();
	    for(int i = 1; i < 100; i++){
	    	series.add(i, averageReducing[i]);
	    }
	    final XYSeriesCollection data = new XYSeriesCollection(series);
	    chart = ChartFactory.createXYLineChart(
	        "XY Series Demo",
	        "% quota reduction", 
	        "% target power loss", 
	        data,
	        PlotOrientation.VERTICAL,
	        true,
	        true,
	        false
	    );

	    final ChartPanel chartPanel = new ChartPanel(chart);
	    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
	    setContentPane(chartPanel);
	    export();

	}

	public void calculate() {
		Random r = new Random();

		for (int quotaChange = 1; quotaChange < 100; quotaChange++) {
			double MAXQUOTACHANGE = (double) quotaChange / 100.d;
			double minG = 0.0;
			double minCount = 0;
			for (int x = 0; x < 10; x++) {
				//for each iteration get a new player distribution
				final int NUMBERPLAYERS = r.nextInt(MAXPLAYER - MINPLAYER) + MINPLAYER; 
				Set<Integer> set = new TreeSet<Integer>();
				for (int i = 0; i < NUMBERPLAYERS; i++)
					set.add(i);

				// ----- ORIGINAL -------
				PowerSet<Integer> pset1 = new PowerSet<Integer>(set);

				int target = r.nextInt(NUMBERPLAYERS);
				int[] weights = new int[NUMBERPLAYERS];
				for (int i = 0; i < weights.length; i++) {
					weights[i] = (int) (r.nextGaussian() * VARIANCE + MEAN);
				}

				int quota = 0;
				int quotaSum = 0;
				for (int w : weights) {
					quotaSum += w;
				}
				quota = r.nextInt(quotaSum);

				// ----- PERTUBATIONS ----
				int qb = (int) (quota * (1 - MAXQUOTACHANGE));
				int[] quotas = new int[C];
				for (int i = 0; i < C; i++) {
					quotas[i] = (int) (qb + (i * (quota * MAXQUOTACHANGE) / C));
				}
				PowerSet<Integer>[] psets = (PowerSet<Integer>[]) Array.newInstance(pset1.getClass(), C);
				for (int i = 0; i < C; i++) {
					psets[i] = new PowerSet<Integer>(set);
				}

				// --------- Execution ----------
				double n = 1 / (Math.pow(2, weights.length - 1));

				ExecutorService es = Executors.newFixedThreadPool(C + 1);
				// reference game
				Future<Double> res1 = es.submit(new BanzhafThread(pset1, target, weights, quota, n)); 
				// pertubated games
				Future<Double>[] results = (Future<Double>[]) Array.newInstance(res1.getClass(), C);
				for (int i = 0; i < results.length; i++) {
					results[i] = es.submit(new BanzhafThread(psets[i], target, weights, quotas[i], n)); 
				}

				try {
					double resg = res1.get();
					double min = resg;
					// calculate the results for the C different quotas
					for (int i = 0; i < results.length; i++) {
						double res = results[i].get();
						if (res / resg < 1 && res < min) // is new minimum?
							min = res;
					}
					// when we have a minimum, save it for statistics
					if (min < resg) {
						minG +=	(min - resg) / resg;	// min / resg * 100;
						minCount++;
					}
					es.shutdown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}

			}
			averageReducing[quotaChange] = ((1 - minG / minCount) - 1) * 100;
			numberOfMinimums[quotaChange] = (int) minCount;
			Log.d("Average minimum: " + (minG / minCount) + ", #: " + minCount + ", quota change: " + MAXQUOTACHANGE);
		}

	}
	
	//http://stackoverflow.com/a/7006052
	public void export(){
		BufferedImage objBufferedImage = chart.createBufferedImage(600,400);
		ByteArrayOutputStream bas = new ByteArrayOutputStream();
		        try {
		            ImageIO.write(objBufferedImage, "png", bas);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }

		byte[] byteArray = bas.toByteArray();

		InputStream in = new ByteArrayInputStream(byteArray);
		BufferedImage image;
		try {
			image = ImageIO.read(in);
			File outputfile = new File("C:\\Users\\Thomas\\Desktop\\image.png");
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void main(final String[] args) {
		final Main main = new Main("XY Series Demo");
		main.pack();
		RefineryUtilities.centerFrameOnScreen(main);
		main.setVisible(true);

	}
}
