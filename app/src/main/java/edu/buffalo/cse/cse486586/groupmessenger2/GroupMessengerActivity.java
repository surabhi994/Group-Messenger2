package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;


public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT[] = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    String myPort;
    int seqNum = 0;
    String msg_id;
    int counter = 0;
    double seq = 0.0;

    PriorityBlockingQueue<String> holdbackQueue;
    HashMap<String, String> backup = new HashMap<String, String>();
    HashMap<String, ArrayList> proposed_seq = new HashMap<String, ArrayList>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button b = (Button) findViewById(R.id.button4);


        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        final Comparator<String> compare = new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                double x = Double.parseDouble(s.split(";")[3]);
                double y = Double.parseDouble(t1.split(";")[3]);

                if (x < y)
                    return -1;
                else if (x == y)
                    return 0;
                else
                    return 1;
            }
        };
        holdbackQueue = new PriorityBlockingQueue<String>(25, compare);

        if (myPort.equals("11108")) {
            msg_id = "A";
            seq = 0.1;
        }
        if (myPort.equals("11112")) {
            msg_id = "B";
            seq = 0.2;
        }

        if (myPort.equals("11116")) {
            msg_id = "C";
            seq = 0.3;
        }

        if (myPort.equals("11120")) {
            msg_id = "D";
            seq = 0.4;
        }

        if (myPort.equals("11124")) {
            msg_id = "E";
            seq = 0.5;
        }


        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String msg = editText.getText().toString();
                editText.setText("");

                counter++;

                String message = (msg_id + counter) + ";" + msg + ";" + myPort + ";" + "0" + ";" + "original";

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, myPort);

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            String msg;

            ServerSocket serverSocket = sockets[0];


            while (true) {
                try {

                    Socket s = serverSocket.accept();


                    DataInputStream di = new DataInputStream(s.getInputStream());
                    DataOutputStream ack = new DataOutputStream(s.getOutputStream());


                    msg = di.readUTF();
                    ack.writeUTF("ACK");
                    String rec[] = msg.split(";");
                    String type = rec[4];
                    String id = rec[0];

                    ArrayList<Double> seq_arr;
                    Log.d("MSG", type);

                    if (type.equals("original")) {
                        seq++;
                        String new_msg = rec[0] + ";" + rec[1] + ";" + rec[2] + ";" + seq + ";" + "proposed";
                        holdbackQueue.add(new_msg);
                        backup.put(rec[0], new_msg);
                        publishProgress(new_msg);
                    } else if (type.equals("proposed")) {
                        String final_msg;


                        Log.d("proposed", msg);
                        if (!proposed_seq.containsKey(id)) {
                            seq_arr = new ArrayList<Double>();
                            proposed_seq.put(id, seq_arr);
                        }
                        if (proposed_seq.containsKey(id)) {
                            ArrayList<Double> a = proposed_seq.get(id);
                            a.add(Double.parseDouble(rec[3]));
                            proposed_seq.put(id, a);


                        }
                        if (proposed_seq.get(id).size() == 5)

                        {

                            Double max = (Double) Collections.max(proposed_seq.get(id));

                            final_msg = id + ";" + rec[1] + ";" + rec[2] + ";" + max + ";" + "accepted";
                            publishProgress(final_msg);
                        }

                    } else if (type.equals("accepted")) {
                        String temp = backup.get(id);
                        holdbackQueue.remove(temp);

                        String final_msg = id + ";" + rec[1] + ";" + rec[2] + ";" + rec[3] + ";" + "deliver";
                        holdbackQueue.add(final_msg);
                        publishProgress(final_msg);

                        Log.d("accepted", "msg");
                    }


                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }//end of while


        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String message = strings[0].trim();
            String rec[] = strings[0].split(";");
            String type = rec[4];
            Log.d("PROGRESS", type);
            if (type.equals("proposed")) {
                Log.d("PROGRESS", "Proposed");
                new ClientTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, myPort);
            }

            if (type.equals("accepted")) {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, myPort);
            }
            if (type.equals("deliver")) {
                while (holdbackQueue.peek() != null) {
                    String x = holdbackQueue.peek();
                    String arr[] = x.split(";");
                    if (arr[4].equals("deliver")) {
                        x = holdbackQueue.poll();
                        String arr1[] = x.split(";");
                        TextView TextView1 = (TextView) findViewById(R.id.textView1);
                        TextView1.append(arr1[1] + "\t\n");
                        TextView TextView2 = (TextView) findViewById(R.id.textView1);
                        TextView2.append("\n");
                        try {
                            SharedPreferences sharedPref = getApplication().getSharedPreferences("Your Pref", Context.MODE_PRIVATE);
                            SharedPreferences.Editor ed = sharedPref.edit();
                            ed.putString(String.valueOf(seqNum), arr1[1]);
                            ed.commit();
                            seqNum++;
                        } catch (Exception e) {
                            Log.v("insert", "fail");
                        }

                    } else
                        break;

                }
            }


            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                for (int i = 0; i < 5; i++) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i]));

                    String sending_msg = msgs[0];

                    DataOutputStream d = new DataOutputStream(socket.getOutputStream());

                    d.writeUTF(sending_msg);
                    d.flush();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String s = in.readUTF();
                    if (s == "ACK") {
                        socket.close();
                    }


                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;

        }
    }

    private class ClientTask2 extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            String port;
            try {
                String msg_to_send = msgs[0];
                String[] array = msgs[0].split(";");
                port = array[2];
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(port));

                DataOutputStream d = new DataOutputStream(socket.getOutputStream());
                d.writeUTF(msg_to_send);
                d.flush();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String s = in.readUTF();//reads msg from server
                if (s == "ACK") {
                    socket.close(); // closes socket
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }
}