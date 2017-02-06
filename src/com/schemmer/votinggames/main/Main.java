package com.schemmer.votinggames.main;

import java.awt.Color;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.util.ShapeUtilities;

import com.schemmer.votinggames.util.BanzhafThread;
import com.schemmer.votinggames.util.Log;
import com.schemmer.votinggames.util.PowerSet;

public class Main extends ApplicationFrame {
	// ------ Weighted Voting Games vars ----
	private static final int MEAN = 200;
	private static final int VARIANCE = 30;
	private static final int REPETITIONS = 100000;
	private static final double MAXQUOTACHANGE = 0.25;
	private static int NUMBERPLAYERS;

	private double[][] raisedPower = new double[100][3];
	private double[][] loweredPower = new double[100][3];
	private static int [] amountRaised = new int[3];
	private static int [] amountLowered = new int[3];

	private JFreeChart chartRaised;
	private JFreeChart chartLowered;

	/*
	 * Idea: calculate the current banzhaf index for a random target. Then
	 * create C variations with quota qi = (qb + i * C/d) Iterate over these new
	 * banzhaf indexes and get the lowest one. If it is below the starting
	 * index, we achieved a lowering of the power of the target player. Do that
	 * x times for each of the MAXQUOTACHANGE (q') values (for example: 100x 20%
	 * quota lowering) and calculate the mean possible lowering for this q'
	 * value. Then increase the allowed lowering and restart
	 */

	public void create(int player){
		NUMBERPLAYERS = player;
		calculate();
		createLowered();
		createRaised();
		export(chartLowered, "lowered");
		export(chartRaised, "raised");
	}
	
	public void createLowered(){
		final XYSeries series[] = new XYSeries[4];
		for(int i = 0; i < series.length; i++){
			series[i] = new XYSeries((MAXQUOTACHANGE*(i+1))+" reduction");
		}
		
		//fill with data
		for (int i = 0; i < loweredPower.length; i++) {
			for (int x = 0; x < 3; x++) {
				if(loweredPower[i][x] != 0){
					series[x].add(i, loweredPower[i][x]);
				}
			}
		}
		//range hack
		series[3].add(0, 0);
		series[3].add(95, 0);
		
		//set datasets from collection
		final XYSeriesCollection data1 = new XYSeriesCollection(series[0]);
		final XYSeriesCollection data2 = new XYSeriesCollection(series[1]);
		final XYSeriesCollection data3 = new XYSeriesCollection(series[2]);
		final XYSeriesCollection data4 = new XYSeriesCollection(series[3]);
		
		chartLowered = ChartFactory.createXYLineChart("" + NUMBERPLAYERS, "Reduction of power, lowered quota",
				"amount of occurence", null, PlotOrientation.VERTICAL, true, true, false);
		
		XYPlot plot = chartLowered.getXYPlot();
		plot.setDataset(0, data1);
		plot.setDataset(1, data2);
		plot.setDataset(2, data3);
		plot.setDataset(3, data4);
		
		//cosmetics
		Shape cross = ShapeUtilities.createDiamond(5);
		
		XYLineAndShapeRenderer rr = new XYLineAndShapeRenderer(); 
		rr.setSeriesLinesVisible(0, false); 
		rr.setSeriesShapesVisible(0, true); 
		rr.setSeriesShape(0, cross);
		rr.setSeriesItemLabelPaint(0, Color.red); 
		plot.setRenderer(0,rr); 
		
		XYLineAndShapeRenderer rr1 = new XYLineAndShapeRenderer(); 
		rr1.setSeriesLinesVisible(0, false); 
		rr1.setSeriesShapesVisible(0, true); 
		rr1.setSeriesShape(0, cross);
		rr1.setSeriesItemLabelPaint(0, Color.green); 
		plot.setRenderer(1,rr1); 
		
		XYLineAndShapeRenderer rr2 = new XYLineAndShapeRenderer(); 
		rr2.setSeriesLinesVisible(0, false); 
		rr2.setSeriesShapesVisible(0, true); 
		rr2.setSeriesShape(0, cross);
		rr2.setSeriesItemLabelPaint(0, Color.blue); 
		plot.setRenderer(2,rr2); 
		
		//hackadiahack 2
		XYLineAndShapeRenderer rr3 = new XYLineAndShapeRenderer(); 
		rr3.setSeriesLinesVisible(0, false); 
		rr3.setSeriesShapesVisible(0, false); 
		rr3.setSeriesVisibleInLegend(0, false);
		plot.setRenderer(3,rr3); 

		
		final ChartPanel chartPanel = new ChartPanel(chartLowered);
		chartPanel.setPreferredSize(new java.awt.Dimension(800, 250));
		setContentPane(chartPanel);
	}
	
	public void createRaised(){
		final XYSeries series[] = new XYSeries[4];
		for(int i = 0; i < series.length; i++){
			series[i] = new XYSeries((MAXQUOTACHANGE*(i+1))+" reduction");
		}

		//fill with data
		for (int i = 0; i < raisedPower.length; i++) {
			for (int x = 0; x < 3; x++) {
				if(raisedPower[i][x] != 0){
					series[x].add(i, raisedPower[i][x]);
				}
			}
		}
		//range hack
		series[3].add(0, 0);
		series[3].add(95, 0);
		
		//set datasets from collection
		final XYSeriesCollection data1 = new XYSeriesCollection(series[0]);
		final XYSeriesCollection data2 = new XYSeriesCollection(series[1]);
		final XYSeriesCollection data3 = new XYSeriesCollection(series[2]);
		final XYSeriesCollection data4 = new XYSeriesCollection(series[3]);
		
		chartRaised = ChartFactory.createXYLineChart("" + NUMBERPLAYERS, "Increase in power, lowered quota",
				"amount of occurence", null, PlotOrientation.VERTICAL, true, true, false);
		
		XYPlot plot = chartRaised.getXYPlot();
		plot.setDataset(0, data1);
		plot.setDataset(1, data2);
		plot.setDataset(2, data3);
		plot.setDataset(3, data4);
		
		//cosmetics
		Shape cross = ShapeUtilities.createDiamond(5);
		
		XYLineAndShapeRenderer rr = new XYLineAndShapeRenderer(); 
		rr.setSeriesLinesVisible(0, false); 
		rr.setSeriesShapesVisible(0, true); 
		rr.setSeriesShape(0, cross);
		rr.setSeriesItemLabelPaint(0, Color.red); 
		plot.setRenderer(0,rr); 
		
		XYLineAndShapeRenderer rr1 = new XYLineAndShapeRenderer(); 
		rr1.setSeriesLinesVisible(0, false); 
		rr1.setSeriesShapesVisible(0, true); 
		rr1.setSeriesShape(0, cross);
		rr1.setSeriesItemLabelPaint(0, Color.green); 
		plot.setRenderer(1,rr1); 
		
		XYLineAndShapeRenderer rr2 = new XYLineAndShapeRenderer(); 
		rr2.setSeriesLinesVisible(0, false); 
		rr2.setSeriesShapesVisible(0, true); 
		rr2.setSeriesShape(0, cross);
		rr2.setSeriesItemLabelPaint(0, Color.blue); 
		plot.setRenderer(2,rr2); 
		
		//hackadiahack 2
		XYLineAndShapeRenderer rr3 = new XYLineAndShapeRenderer(); 
		rr3.setSeriesLinesVisible(0, false); 
		rr3.setSeriesShapesVisible(0, false); 
		rr3.setSeriesVisibleInLegend(0, false);
		plot.setRenderer(3,rr3); 

		
		final ChartPanel chartPanel = new ChartPanel(chartRaised);
		chartPanel.setPreferredSize(new java.awt.Dimension(800, 250));
//		setContentPane(chartPanel);
	}
	
	public Main(final String title) {
		super(title);
	}

	public void calculate() {
		Random r = new Random();
		int count = 0;

		for (int i = 0; i < 3; i++) {
			for (int x = 0; x < REPETITIONS; x++) {
				count++;
				// for each iteration get a new player distribution
				final int EQUALIZER = (int) Math.pow(2, NUMBERPLAYERS - 1);
				Set<Integer> set = new TreeSet<Integer>();
				for (int i2 = 0; i2 < NUMBERPLAYERS; i2++)
					set.add(i2);

				// ----- ORIGINAL -------
				PowerSet<Integer> pset1 = new PowerSet<Integer>(set);

				int target = r.nextInt(NUMBERPLAYERS);
				int[] weights = new int[NUMBERPLAYERS];
				for (int i2 = 0; i2 < weights.length; i2++) {
					weights[i2] = (int) (r.nextGaussian() * VARIANCE + MEAN);
				}

				int quota = 0;
				int quotaSum = 0;
				for (int w : weights) {
					quotaSum += w;
				}
				quota = r.nextInt(quotaSum);

				// ----- PERTUBATIONS ----
				int quotaChange = (int) (quota * (1 - MAXQUOTACHANGE*(i+1)));
				PowerSet<Integer> pset2 = new PowerSet<Integer>(set);

				// --------- Execution ----------
				double n = 1 / (Math.pow(2, weights.length - 1));

				ExecutorService es = Executors.newFixedThreadPool(2);
				// reference game
				Future<Double> res1 = es.submit(new BanzhafThread(pset1, target, weights, quota, n));
				// pertubated games
				Future<Double> res2 = es.submit(new BanzhafThread(pset2, target, weights, quotaChange, n));

				DecimalFormat df = new DecimalFormat("#.####");
				df.setRoundingMode(RoundingMode.CEILING);
				try {
					double resg1 = res1.get();
					double resg2 = res2.get();
					if(resg1 != 0.0){
						if (resg2 - resg1 > 0.0 ) {
	//						Log.d("int: "+(int) ((resg2 / resg1) / EQUALIZER * 100)+", normal "+((resg2 / resg1) / EQUALIZER * 100));
							amountRaised[i]++;
							raisedPower[(int) ((resg2 / resg1) / EQUALIZER * 100)][i]++;
						}
						if (resg2 - resg1 < 0.0) {
							amountLowered[i]++;
//							Log.d("Lowered: "+((resg2- resg1) / resg1 * -100 - 1)+", resg2: "+resg2+", resg1: "+resg1);
							loweredPower[(int) (((resg2- resg1) / resg1)  * -100 - 1)][i]++;
						}
					}
					es.shutdown();
					if(x%(REPETITIONS/10) == 0)
						Log.d("X: "+x);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}

			} // repitions loop
		}
		Log.d("Raised:");
		Log.d(amountRaised);
		Log.d("Lowered:");
		Log.d(amountLowered);

	}

	// http://stackoverflow.com/a/7006052
	public void export(JFreeChart chart, String title) {
		BufferedImage objBufferedImage = chart.createBufferedImage(800, 250);
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
			File outputfile = new File("C:\\Users\\Thomas\\Desktop\\"+title+""+NUMBERPLAYERS+".png");
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) {
		for(int i = 0; i < 3; i++){
			final Main main = new Main("XY Series Demo");
			Log.d("test "+(5+i*5));
			main.create(5+i*5);
			main.pack();
			RefineryUtilities.centerFrameOnScreen(main);
			main.setVisible(true);
			
		}
	}
}
