package com.example.nimolee.nm_l2;

import android.graphics.Color;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    double coefInerc;
    double learnSpeed;

    class Neiron {
        private double[] w;
        double S;
        double delta;
        double[] yi;

        Neiron(int size) {
            w = new double[size];
            Random random = new Random();
            for (int i = 0; i < size; i++) {
                w[i] = random.nextDouble();
            }
        }

        double iteration(double[] x) {
            S = 0;
            for (int i = 0; i < x.length; i++)
                S += x[i] * w[i];
            double t = 1;

            return 1 / (t + Math.exp(-S * coefInerc));
        }

        void configure() {
            for (int i = 0; i < w.length; i++) {
                w[i] += w[i] * yi[i] * delta * learnSpeed;
            }
        }

        double[] getW() {
            return w;
        }
    }

    class Pers {
        Neiron out1;
        List<Neiron[]> neir;
        List<Double> yOut = new ArrayList<>();
        List<Double> yrOut = new ArrayList<>();

        Pers(int width, int[] deph) {
            neir = new ArrayList<>();
            for (int i = 1; i < width; i++) {
                neir.add(new Neiron[deph[i]]);
                for (int j = 0; j < deph[i]; j++) {
                    neir.get(i - 1)[j] = new Neiron(deph[i - 1]);
                }
            }
            out1 = new Neiron(deph[deph.length - 1]);
        }


        double iteration(double[] in) {
            double[] prev_step = in;
            double[] next_step;
            for (int i = 0; i < neir.size(); i++) {
                next_step = new double[neir.get(i)[0].w.length];
                for (int j = 0; j < neir.get(i).length; j++) {
                    next_step[j] = neir.get(i)[j].iteration(prev_step);
                }
                prev_step = next_step;
            }
            return out1.iteration(prev_step);
        }

        void learn(double[] in, double y) {
            List<double[]> yr = new ArrayList<>();
            for (int i = 0; i < neir.size(); i++) {
                yr.add(new double[neir.get(i).length]);
            }
            double[] prev_step = in;
            double[] next_step;
            for (int i = 0; i < neir.size(); i++) {
                next_step = new double[neir.get(i).length];
                for (int j = 0; j < neir.get(i).length; j++) {
                    next_step[j] = neir.get(i)[j].iteration(prev_step);
                    yr.get(i)[j] = next_step[j];
                }
                prev_step = next_step;
            }
            double yl = out1.iteration(prev_step);
            yOut.add(y);
            yrOut.add(yl);
            out1.delta = coefInerc * (Math.exp(out1.S * coefInerc)) / Math.pow(-(Math.exp(out1.S * coefInerc) + 1), 2) * (y - yl);
            for (int i = 0; i < neir.get(neir.size() - 1).length; i++) {
                neir.get(neir.size() - 1)[i].delta =
                        yr.get(yr.size() - 1)[i] * out1.delta * out1.getW()[i];
            }
            double sum_mul;
            for (int i = neir.size() - 2; i > -1; i--) {
                for (int j = 0; j < neir.get(i).length; j++) {
                    sum_mul = 0;
                    for (int j1 = 0; j1 < neir.get(i + 1).length; j1++) {
                        sum_mul += neir.get(i + 1)[j1].delta * neir.get(i + 1)[j1].getW()[j];
                    }
                    neir.get(i)[j].delta = yr.get(i)[j] * sum_mul;
                }
            }
            for (int i = 0; i < neir.size(); i++) {
                for (int j = 0; j < neir.get(i).length; j++) {
                    neir.get(i)[j].yi = new double[neir.get(i)[j].getW().length];
                    for (int k = 0; k < neir.get(i)[j].getW().length; k++) {
                        if (i < 1) {
                            neir.get(i)[j].yi[k] = in[k];
                        } else {
                            neir.get(i)[j].yi[k] = yr.get(i - 1)[k];
                        }
                    }
                }
            }
            out1.yi = yr.get(yr.size() - 1);
            for (int i = 0; i < neir.size(); i++) {
                for (int j = 0; j < neir.get(i).length; j++) {
                    neir.get(i)[j].configure();
                }
            }
            out1.configure();
        }

        void printWight() {
            TextView outTV = (TextView) findViewById(R.id.resultTV);
            String outS = "";
            for (Neiron[] a : neir) {
                for (Neiron b : a) {
                    outS += "| ";
                    for (double c : b.getW()) {
                        outS += String.format(Locale.ENGLISH, "%.3f ", c);
                    }
                    outS += " |";
                }
                outS += "\n";
            }
            outS += "| ";
            for (int i = 0; i < out1.getW().length; i++) {
                outS += String.format(Locale.ENGLISH, "%.3f ", out1.getW()[i]);
            }
            outS += " |";
            outTV.setText(outS);
        }

        void drawGraph() {
            GraphView graphView = (GraphView) findViewById(R.id.resultGV);
            LineGraphSeries<DataPoint> y1;
            LineGraphSeries<DataPoint> y2;
            DataPoint[] y1D = new DataPoint[yOut.size()];
            DataPoint[] y2D = new DataPoint[yrOut.size()];
            for (int i = 0; i < y1D.length; i++) {
                y1D[i] = new DataPoint(i, yOut.get(i));
            }
            for (int i = 0; i < y2D.length; i++) {
                y2D[i] = new DataPoint(i, yrOut.get(i));
            }
            y1 = new LineGraphSeries<>(y1D);
            y2 = new LineGraphSeries<>(y2D);
            y1.setColor(Color.CYAN);
            y2.setColor(Color.MAGENTA);
            y1.setTitle("Y*");
            y2.setTitle("Y");
            graphView.addSeries(y1);
            graphView.addSeries(y2);
            graphView.getLegendRenderer().setVisible(true);
            graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);
            graphView.getViewport().setScalable(true);
            graphView.getViewport().setScalableY(true);
        }
    }


    public void onClick(View view) {

        File file = new File("/storage/emulated/0/Download/variant_4.csv");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str;
            List<double[]> inputData = new ArrayList<>();
            List<Double> y = new ArrayList<>();
            br.readLine();
            while ((str = br.readLine()) != null) {
                inputData.add(parse_string(str));
                y.add(Double.parseDouble(str.split(";")[str.split(";").length - 1]));
            }
            double max = y.get(0);
            for (int i = 1; i < y.size(); i++) {
                if (max < y.get(i)) {
                    max = y.get((i));
                }
            }
            max *= 2.;
            for (int i = 0; i < y.size(); i++) {
                y.set(i, y.get(i) / max + 0.5);
            }
            String dephS = ((EditText) findViewById(R.id.setting_deph)).getText().toString();
            learnSpeed = Double.parseDouble(((EditText) findViewById(R.id.learnSpeed)).getText().toString());
            coefInerc = Double.parseDouble(((EditText) findViewById(R.id.coefInerc)).getText().toString());
            int width = dephS.split(" ").length + 1;
            int[] deph = new int[width];
            int ep = Integer.parseInt(((EditText) findViewById(R.id.setting_ep)).getText().toString());
            for (int i = 0; i < width; i++) {
                if (i > 0) {
                    deph[i] =
                            Integer.parseInt(dephS.split(" ")[i - 1]);
                } else {
                    deph[i] = inputData.get(i).length;
                }
            }
            Pers pers = new Pers(width, deph);
            for (int i = 0; i < ep; i++) {
                for (int j = 0; j < inputData.size(); j++) {
                    pers.learn(inputData.get(j), y.get(j));
                }
            }
            setContentView(R.layout.result);
            pers.printWight();
            pers.drawGraph();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    double[] parse_string(String str) {
        double[] out = new double[str.split(";").length - 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = Double.parseDouble(str.split(";")[i]);
        }
        return out;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
    }
}
