package edu.cs4730.nearbyconnectiondemo;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * A simple {@link Fragment} subclass.
 */
public class DiscoveryFragment extends Fragment {
    String TAG = "DiscoveryFragment";
    TextView logger;
    Boolean mIsDiscovering = false;
    String UserNickName = "DiscoveryNearbyDemo";

    String ConnectedEndPointId;

    public DiscoveryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View myView = inflater.inflate(R.layout.fragment_discovery, container, false);
        logger = myView.findViewById(R.id.di_output);
        myView.findViewById(R.id.start_discovery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsDiscovering)
                  stopDiscovering();//in discovery mode, turn it off
                else
                  startDiscovering();
            }
        });
        myView.findViewById(R.id.end_discovery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectedEndPointId.compareTo("") !=0 ) { //connected to someone
                    Nearby.getConnectionsClient(getContext()).disconnectFromEndpoint(ConnectedEndPointId);
                    ConnectedEndPointId = "";
                }
                if (mIsDiscovering) {
                    stopDiscovering();
                }
            }
        });
        return myView;
    }

    /**
     * Sets the device to discovery mode.
     */
    protected void startDiscovering() {
        Nearby.getConnectionsClient(getContext()).
            startDiscovery(
                MainActivity.ServiceId,   //id for the service to be discovered.  ie, what are we looking for.

                new EndpointDiscoveryCallback() {  //callback when we discovery that endpoint.
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                        logthis("We found an endpoint " + endpointId + " name is " + info.getEndpointName());
                        makeConnection(endpointId);
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        logthis("End point lost  " + endpointId);
                    }
                },

                new DiscoveryOptions(MainActivity.STRATEGY))  //options for discovery.
            .addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        mIsDiscovering = true;
                        logthis("We have started discovery.");
                    }
                })
            .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mIsDiscovering = false;
                        logthis("We failed to start discovery.");
                        e.printStackTrace();
                    }
                });

    }

    //the connection callback, both discovery and advertise use the same one.
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
        new ConnectionLifecycleCallback() {

            @Override
            public void onConnectionInitiated(
                String endpointId, ConnectionInfo connectionInfo) {
                // Automatically accept the connection on both sides.
                Nearby.getConnectionsClient(getContext()).acceptConnection(endpointId, //mPayloadCallback);
                    new PayloadCallback() {
                        @Override
                        public void onPayloadReceived(String endpointId, Payload payload) {
                            //input makes sense, get a stream like below.  how to send???
                            //To send payloads to a connected endpoint, call sendPayload().  not from this method.  should work else where.
                            /*Stream
                            Stream payloads are suitable when you want to send large amounts of data that is generated on the fly,
                            such as an audio stream. Create a STREAM Payload by calling Payload.fromStream(), passing in either an
                            InputStream or a ParcelFileDescriptor. On the recipient, call payload.asStream().asInputStream()
                            or payload.asStream().asParcelFileDescriptor().*/

                            if (payload.getType() == Payload.Type.BYTES) {
                                String stuff = new String(payload.asBytes());
                                logthis("Received data is " + stuff);
                                if (stuff.startsWith("Hi")) {
                                    send("Good to meet your Advertiser");
                                }
                            } else if (payload.getType() == Payload.Type.FILE)
                                logthis("We got a file.  not handled");
                            else if (payload.getType() == Payload.Type.STREAM)
                                //payload.asStream().asInputStream()
                                logthis("We got a stream, not handled");
                        }

                        @Override
                        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                            //if stream or file, we need to know when the transfer has finished.  ignoring this right now.
                        }
                    });
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution result) {
                switch (result.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        // We're connected! Can now start sending and receiving data.
                        ConnectedEndPointId = endpointId;
                        send("Hi from Discovery");
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        // The connection was rejected by one or both sides.
                        break;
                    case ConnectionsStatusCodes.STATUS_ERROR:
                        // The connection broke before it was able to be accepted.
                        break;
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
                ConnectedEndPointId = "";
            }
        };



    public void makeConnection(String endpointId) {
        Nearby.getConnectionsClient(getContext())
           .requestConnection(
            UserNickName,   //human readable name for the local endpoint.  if null/empty, uses device name or model.
            endpointId,
            mConnectionLifecycleCallback)
            .addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        // We successfully requested a connection. Now both sides
                        // must accept before the connection is established.
                    }
                })
            .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Nearby Connections failed to request the connection.
                    }
                });

    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     *
     */
    protected void send(String data) {

        //basic error checking
        if (ConnectedEndPointId.compareTo("") == 0)   //empty string, no connection
            return;

        Payload payload = Payload.fromBytes(data.getBytes());

        // sendPayload (List<String> endpointIds, Payload payload)  if more then one connection allowed.
        Nearby.getConnectionsClient(getContext()).
            sendPayload(ConnectedEndPointId,  //end point to end to
                payload)   //the actual payload of data to send.
            .addOnSuccessListener(new OnSuccessListener<Void>() {  //don't know if need this one.
                @Override
                public void onSuccess(Void aVoid) {
                    logthis("Message send successfully.");
                }
            })
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    logthis("Message send completed.");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    logthis("Message send failed.");
                    e.printStackTrace();
                }
            });
    }


    @Override
    public void onStop() {
        super.onStop();
        stopDiscovering();
    }

    /** Stops discovery. */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        Nearby.getConnectionsClient(getContext()).stopAdvertising();
    }


    public void logthis(String msg) {
        logger.append(msg + "\n");
        Log.d(TAG, msg);
    }
}
