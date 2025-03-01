package com.kimikevin.routing;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kimikevin.Feature;
import com.kimikevin.Point;
import com.kimikevin.Rectangle;
import com.kimikevin.RouteGuideGrpc;
import com.kimikevin.RouteNote;
import com.kimikevin.RouteSummary;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class MainActivity extends AppCompatActivity {
    private EditText hostEdit;
    private EditText portEdit;
    private Button startRouteGuideButton;
    private Button exitRouteGuideButton;
    private Button getFeatureButton;
    private Button listFeaturesButton;
    private Button recordRouteButton;
    private Button routeChatButton;
    private TextView resultText;
    private ManagedChannel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hostEdit = findViewById(R.id.host_edit_text);
        portEdit = findViewById(R.id.port_edit_text);
        startRouteGuideButton = findViewById(R.id.start_route_guide_button);
        exitRouteGuideButton = findViewById(R.id.exit_route_guide_button);
        getFeatureButton = findViewById(R.id.get_feature_button);
        listFeaturesButton = findViewById(R.id.list_features_button);
        recordRouteButton = findViewById(R.id.record_route_button);
        routeChatButton = findViewById(R.id.route_chat_button);
        resultText = findViewById(R.id.result_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());
        disableButtons();
    }

    public void startRouteGuide(View view) {
        String host = hostEdit.getText().toString();
        String portStr = portEdit.getText().toString();
        int port = TextUtils.isEmpty(portStr) ? 0 : Integer.parseInt(portStr);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(hostEdit.getWindowToken(), 0);
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        hostEdit.setEnabled(false);
        portEdit.setEnabled(false);
        startRouteGuideButton.setEnabled(false);
        enableButtons();
    }

    public void exitRouteGuide(View view) {
        channel.shutdown();
        disableButtons();
        hostEdit.setEnabled(true);
        portEdit.setEnabled(true);
        startRouteGuideButton.setEnabled(true);
    }

    public void getFeature(View view) {
        setResultText("");
        disableButtons();
        new GrpcTask(new GetFeatureRunnable(), channel, this).execute();
    }

    public void listFeatures(View view) {
        setResultText("");
        disableButtons();
        new GrpcTask(new ListFeaturesRunnable(), channel, this).execute();
    }

    public void recordRoute(View view) {
        setResultText("");
        disableButtons();
        new GrpcTask(new RecordRouteRunnable(), channel, this).execute();
    }

    public void routeChat(View view) {
        setResultText("");
        disableButtons();
        new GrpcTask(new RouteChatRunnable(), channel, this).execute();
    }

    private void setResultText(String text) {
        resultText.setText(text);
    }

    private void disableButtons() {
        getFeatureButton.setEnabled(false);
        listFeaturesButton.setEnabled(false);
        recordRouteButton.setEnabled(false);
        routeChatButton.setEnabled(false);
        exitRouteGuideButton.setEnabled(false);
    }

    private void enableButtons() {
        exitRouteGuideButton.setEnabled(true);
        getFeatureButton.setEnabled(true);
        listFeaturesButton.setEnabled(true);
        recordRouteButton.setEnabled(true);
        routeChatButton.setEnabled(true);
    }

    private static class GrpcTask extends AsyncTask<Void, Void, String> {
        private final GrpcRunnable grpcRunnable;
        private final ManagedChannel channel;
        private final WeakReference<MainActivity> activityReference;

        GrpcTask(GrpcRunnable grpcRunnable, ManagedChannel channel, MainActivity activity) {
            this.grpcRunnable = grpcRunnable;
            this.channel = channel;
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String logs =
                        grpcRunnable.run(
                                RouteGuideGrpc.newBlockingStub(channel), RouteGuideGrpc.newStub(channel)
                        );
                return "Success!\n" + logs;
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return "Failed... :\n" + sw;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            activity.setResultText(result);
            activity.enableButtons();
        }
    }

    private interface GrpcRunnable {
        /** Perform a grpcRunnable and return all the logs. */
        String run(RouteGuideGrpc.RouteGuideBlockingStub blockingStub, RouteGuideGrpc.RouteGuideStub asyncStub) throws Exception;
    }

    private static class GetFeatureRunnable implements GrpcRunnable {
        @Override
        public String run(RouteGuideGrpc.RouteGuideBlockingStub blockingStub, RouteGuideGrpc.RouteGuideStub asyncStub) {
            return getFeature(blockingStub);
        }

        /** Blocking unary call example. Calls getFeature and prints the response. */
        private String getFeature(RouteGuideGrpc.RouteGuideBlockingStub blockingStub)
                throws StatusRuntimeException {
            StringBuffer logs = new StringBuffer();
            appendLogs(logs, "*** GetFeature: lat={0} lon={1}", 409146138, -746188906);

            Point request = Point.newBuilder().setLatitude(409146138).setLongitude(-746188906).build();

            Feature feature;
            feature = blockingStub.getFeature(request);
            if (RouteGuideUtil.exists(feature)) {
                appendLogs(
                        logs,
                        "Found feature called \"{0}\" at {1}, {2}",
                        feature.getName(),
                        RouteGuideUtil.getLatitude(feature.getLocation()),
                        RouteGuideUtil.getLongitude(feature.getLocation()));
            } else {
                appendLogs(
                        logs,
                        "Found no feature at {0}, {1}",
                        RouteGuideUtil.getLatitude(feature.getLocation()),
                        RouteGuideUtil.getLongitude(feature.getLocation()));
            }
            return logs.toString();
        }
    }

    private static class ListFeaturesRunnable implements GrpcRunnable {
        @Override
        public String run(RouteGuideGrpc.RouteGuideBlockingStub blockingStub, RouteGuideGrpc.RouteGuideStub asyncStub) {
            return listFeatures(blockingStub);
        }

        /**
         * Blocking server-streaming example. Calls listFeatures with a rectangle of interest. Prints
         * each response feature as it arrives.
         */
        private String listFeatures(
                RouteGuideGrpc.RouteGuideBlockingStub blockingStub)
                throws StatusRuntimeException {
            StringBuffer logs = new StringBuffer("Result: ");
            appendLogs(
                    logs,
                    "*** ListFeatures: lowLat={0} lowLon={1} hiLat={2} hiLon={3}",
                    400000000,
                    -750000000,
                    420000000,
                    -730000000);

            Rectangle request =
                    Rectangle.newBuilder()
                            .setLo(Point.newBuilder().setLatitude(400000000).setLongitude(-750000000).build())
                            .setHi(Point.newBuilder().setLatitude(420000000).setLongitude(-730000000).build())
                            .build();
            Iterator<Feature> features;
            features = blockingStub.listFeatures(request);

            while (features.hasNext()) {
                Feature feature = features.next();
                appendLogs(logs, feature.toString());
            }
            return logs.toString();
        }
    }

    private static class RecordRouteRunnable implements GrpcRunnable {
        private Throwable failed;

        @Override
        public String run(RouteGuideGrpc.RouteGuideBlockingStub blockingStub, RouteGuideGrpc.RouteGuideStub asyncStub)
                throws Exception {
            List<Point> points = new ArrayList<>();
            points.add(Point.newBuilder().setLatitude(407838351).setLongitude(-746143763).build());
            points.add(Point.newBuilder().setLatitude(408122808).setLongitude(-743999179).build());
            points.add(Point.newBuilder().setLatitude(413628156).setLongitude(-749015468).build());
            return recordRoute(points, asyncStub);
        }

        /**
         * Async client-streaming example. Sends {@code numPoints} randomly chosen points from {@code
         * features} with a variable delay in between. Prints the statistics when they are sent from the
         * server.
         */
        private String recordRoute(List<Point> points, RouteGuideGrpc.RouteGuideStub asyncStub)
                throws InterruptedException, RuntimeException {
            final StringBuffer logs = new StringBuffer();
            appendLogs(logs, "*** RecordRoute");

            final CountDownLatch finishLatch = new CountDownLatch(1);
            StreamObserver<RouteSummary> responseObserver =
                    new StreamObserver<RouteSummary>() {
                        @Override
                        public void onNext(RouteSummary summary) {
                            appendLogs(
                                    logs,
                                    "Finished trip with {0} points. Passed {1} features. "
                                            + "Travelled {2} meters. It took {3} seconds.",
                                    summary.getPointCount(),
                                    summary.getFeatureCount(),
                                    summary.getDistance(),
                                    summary.getElapsedTime());
                        }

                        @Override
                        public void onError(Throwable t) {
                            failed = t;
                            finishLatch.countDown();
                        }

                        @Override
                        public void onCompleted() {
                            appendLogs(logs, "Finished RecordRoute");
                            finishLatch.countDown();
                        }
                    };

            StreamObserver<Point> requestObserver = asyncStub.recordRoute(responseObserver);
            try {
                // Send numPoints points randomly selected from the points list.
                Random rand = new Random();
                for (int i = 0; i < 5; ++i) {
                    int index = rand.nextInt(points.size());
                    Point point = points.get(index);
                    appendLogs(
                            logs,
                            "Visiting point {0}, {1}",
                            RouteGuideUtil.getLatitude(point),
                            RouteGuideUtil.getLongitude(point));
                    requestObserver.onNext(point);
                    // Sleep for a bit before sending the next one.
                    Thread.sleep(rand.nextInt(1000) + 500);
                    if (finishLatch.getCount() == 0) {
                        // RPC completed or error before we finished sending.
                        // Sending further requests won't error, but they will just be thrown away.
                        break;
                    }
                }
            } catch (RuntimeException e) {
                // Cancel RPC
                requestObserver.onError(e);
                throw e;
            }
            // Mark the end of requests
            requestObserver.onCompleted();

            // Receiving happens asynchronously
            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                throw new RuntimeException(
                        "Could not finish rpc within 1 minute, the server is likely down");
            }

            if (failed != null) {
                throw new RuntimeException(failed);
            }
            return logs.toString();
        }

    }

    private static class RouteChatRunnable implements GrpcRunnable {
        private Throwable failed;

        @Override
        public String run(RouteGuideGrpc.RouteGuideBlockingStub blockingStub, RouteGuideGrpc.RouteGuideStub asyncStub)
                throws Exception {
            return routeChat(asyncStub);
        }

        /**
         * Bi-directional example, which can only be asynchronous. Send some chat messages, and print
         * any chat messages that are sent from the server.
         */
        private String routeChat(RouteGuideGrpc.RouteGuideStub asyncStub)
                throws InterruptedException, RuntimeException {
            final StringBuffer logs = new StringBuffer();
            appendLogs(logs, "*** RouteChat");
            final CountDownLatch finishLatch = new CountDownLatch(1);
            StreamObserver<RouteNote> requestObserver =
                    asyncStub.routeChat(
                            new StreamObserver<RouteNote>() {
                                @Override
                                public void onNext(RouteNote note) {
                                    appendLogs(
                                            logs,
                                            "Got message \"{0}\" at {1}, {2}",
                                            note.getMessage(),
                                            note.getLocation().getLatitude(),
                                            note.getLocation().getLongitude());
                                }

                                @Override
                                public void onError(Throwable t) {
                                    failed = t;
                                    finishLatch.countDown();
                                }

                                @Override
                                public void onCompleted() {
                                    appendLogs(logs, "Finished RouteChat");
                                    finishLatch.countDown();
                                }
                            });

            try {
                RouteNote[] requests = {
                        newNote("First message", 0, 0),
                        newNote("Second message", 0, 1),
                        newNote("Third message", 1, 0),
                        newNote("Fourth message", 1, 1)
                };

                for (RouteNote request : requests) {
                    appendLogs(
                            logs,
                            "Sending message \"{0}\" at {1}, {2}",
                            request.getMessage(),
                            request.getLocation().getLatitude(),
                            request.getLocation().getLongitude());
                    requestObserver.onNext(request);
                }
            } catch (RuntimeException e) {
                // Cancel RPC
                requestObserver.onError(e);
                throw e;
            }
            // Mark the end of requests
            requestObserver.onCompleted();

            // Receiving happens asynchronously
            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                throw new RuntimeException(
                        "Could not finish rpc within 1 minute, the server is likely down");
            }

            if (failed != null) {
                throw new RuntimeException(failed);
            }

            return logs.toString();
        }
    }

    private static void appendLogs(StringBuffer logs, String msg, Object... params) {
        if (params.length > 0) {
            logs.append(MessageFormat.format(msg, params));
        } else {
            logs.append(msg);
        }
        logs.append("\n");
    }

    private static RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder()
                .setMessage(message)
                .setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build())
                .build();
    }
}