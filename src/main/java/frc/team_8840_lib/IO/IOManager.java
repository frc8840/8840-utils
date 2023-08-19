package frc.team_8840_lib.IO;

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import edu.wpi.first.networktables.NetworkTableEntry;
import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.console.Logger.LogType;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.replay.Replayable;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.logging.Loggable;

public class IOManager implements Loggable {

    private static IOManager instance;

    public static IOManager getInstance() {
        if (instance == null) {
            instance = new IOManager();
        }

        return instance;
    }

    public static void init() {
        if (instance != null) return;
        
        instance = new IOManager();

        //IOPowerDistribution.init();
    }

    public static void close() {
        getInstance().exit();
    }

    public static void addIO(IOLayer layer) {
        getInstance().addIOLayer(layer);
    }

    private ArrayList<IOLayer> ioLayers = new ArrayList<IOLayer>();

    //This may need to be disabled in competition since it can use up a lot of bandwidth potentially.
    private boolean outputingToComms = true;

    private IOManager() {
        ioLayers = new ArrayList<IOLayer>();

        Logger.addClassToBeAutoLogged(this);
    }

    public void addIOLayer(IOLayer layer) {
        ioLayers.add(layer);
    }

    private HashMap<String, Long> recordedWriteChanges;

    @AutoLog( name = "IO" )
    public byte[] readAndSendIOInformation() {
        HashMap<String, ArrayList<IOInfo>> data = new HashMap<>();

        ArrayList<IOWriteInfo> ioWriteInfo = new ArrayList<>();

        HashMap<String, Integer> layerCount = new HashMap<>();

        if (recordedWriteChanges == null) {
            recordedWriteChanges = new HashMap<>();
        }

        for (IOLayer layer : this.ioLayers) {
            IOPermission perms = IOPermission.NONE;

            //get the count of the write methods.
            if (!layerCount.containsKey(layer.getBaseName())) {
                layerCount.put(layer.getBaseName(), -1);
            }

            layerCount.put(layer.getBaseName(), layerCount.get(layer.getBaseName()) + 1);

            boolean hasRead = false;
            boolean hasWrite = false;

            if (outputingToComms) {
                CommunicationManager.getInstance()
                    .updateInfo(
                        "IO", 
                        layer.getBaseName() + "/.info/p",
                        perms.shortName()
                    )
                    .updateInfo(
                        "IO", 
                        layer.getBaseName() + "/" + layerCount.get(layer.getBaseName()) + "/.info/real", 
                        layer.isReal()
                    );
            }

            for (Method method : layer.getClass().getMethods()) {                
                IOMethod iomethod = method.getAnnotation(IOMethod.class);

                if (iomethod == null) {
                    continue;
                }

                if (!iomethod.toNT()) continue;

                String key = layer.getBaseName() + "/" + layerCount.get(layer.getBaseName()) + "/" + iomethod.name();

                /*
                We don't need the write methods since they're supposed to be invoked
                when writing. All we're doing here is reading the methods so
                it's not needed.
                */
                if (iomethod.method_type() == IOMethodType.WRITE) {
                    hasWrite = true;

                    IOWriteInfo writeInfo = new IOWriteInfo(
                        key,
                        layer,
                        method,
                        iomethod.value_type()
                    );
                    
                    if (outputingToComms) {
                        //just write the type of the value.
                        CommunicationManager.getInstance()
                            .updateInfo("IO", key + "/t", iomethod.value_type().toString());
                    }

                    ioWriteInfo.add(writeInfo);

                    continue;
                } else if (iomethod.method_type() == IOMethodType.READ) {
                    hasRead = true;
                }

                if (!data.containsKey(layer.getBaseName())) {
                    data.put(layer.getBaseName(), new ArrayList<IOInfo>());
                }

                try {
                    if (iomethod.method_type() == IOMethodType.READ) {
                        data.get(layer.getBaseName()).add(
                            new IOInfo(
                                key,
                                method.invoke(layer),
                                iomethod.value_type(),
                                perms,
                                layer.isReal()
                            )
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalAccessError("[IOManager] All @IOMethod methods must be declared as public, or there may have been an issue with the method.");
                }
            }

            //Quickly check perms to make sure it has all of the methods.
            //Unless it's a replayable, since it's a bit different.
            if (!(layer instanceof Replayable)) {
                if (hasRead && hasWrite) {
                    perms = IOPermission.READ_WRITE;
                } else {
                    perms = IOPermission.READ;
                }
            }
        }

        if (outputingToComms) {
            for (String _key : data.keySet()) {
                for (int i = 0; i < data.get(_key).size(); i++) {
                    Object preValue = data.get(_key).get(i).value;
                    String key = data.get(_key).get(i).name;

                    try {
                        switch (data.get(_key).get(i).type) {
                            case DOUBLE:
                                CommunicationManager.getInstance()
                                    .updateInfo("IO", key + "/v", (double) preValue);
                                    break;
                            case INT:
                                CommunicationManager.getInstance()
                                    .updateInfo("IO", key + "/v", (int) preValue);
                                break;
                            case STRING:
                                CommunicationManager.getInstance()
                                    .updateInfo("IO", key + "/v", (String) preValue);
                                break;
                            case BOOLEAN:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/v", (boolean) preValue);
                                break;
                            case BYTE_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/v", (byte[]) preValue);
                                break;
                            case DOUBLE_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/v", (double[]) preValue);
                                break;
                            case LONG_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/v", (long[]) preValue);
                                break;
                            case STRING_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/v", (String[]) preValue);
                                break;
                            case BOOLEAN_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/v", (boolean[]) preValue);
                                break;
                            case NONE:
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException("[IOManager] There was an issue parsing the value returned from the read method " + data.get(key).get(i).name + ". Are the types matched up?");
                    }

                    CommunicationManager.getInstance()
                        .updateInfo(
                            "IO", 
                            key + "/t",
                            data.get(_key).get(i).type.name()
                        );  
                }
            }


            for (IOWriteInfo writeInfo : ioWriteInfo) {
                String key = writeInfo.name;

                NetworkTableEntry entry = CommunicationManager.getInstance().get("IO", key + "/w");

                if (entry == null || !entry.exists()) {
                    continue;
                }

                long lastEdited = entry.getLastChange();
                long recordedLastEdited = recordedWriteChanges.getOrDefault(key, 0L);

                if (lastEdited == recordedLastEdited) {
                    continue;
                }

                recordedWriteChanges.put(key, lastEdited);

                Method method = writeInfo.method;
                IOLayer layer = writeInfo.layer;

                IOValue valueType = writeInfo.type;

                Logger.Log("[IOManager] Recieved IO Update for " + key + ".");

                try {
                    switch (valueType) {
                        case DOUBLE:
                            method.invoke(layer, (double) entry.getDouble(0d));
                            break;
                        case INT:
                            method.invoke(layer, (int) entry.getInteger(0L));
                            break;
                        case STRING:
                            method.invoke(layer, (String) entry.getString("ERROR"));
                            break;
                        case BOOLEAN:
                            method.invoke(layer, (boolean) entry.getBoolean(false));
                            break;
                        case BYTE_ARRAY:
                            method.invoke(layer, (byte[]) entry.getRaw(new byte[0]));
                            break;
                        case DOUBLE_ARRAY:
                            method.invoke(layer, (double[]) entry.getDoubleArray(new double[0]));
                            break;
                        case LONG_ARRAY:
                            method.invoke(layer, (long[]) entry.getIntegerArray(new long[0]));
                            break;
                        case STRING_ARRAY:
                            method.invoke(layer, (Object[]) entry.getStringArray(new String[0]));
                            break;
                        case BOOLEAN_ARRAY:
                            method.invoke(layer, (boolean[]) entry.getBooleanArray(new boolean[0]));
                            break;
                        case NONE:
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("[IOManager] There was an issue parsing the value returned from the read method " + key + ". Are the types matched up?");
                }
            }
        }

        //TODO: Allow IO to be interpreted.
        // ArrayList<Byte> prelog = new ArrayList<>();

        // for (String key : data.keySet()) {
        //     for (int i = 0; i < data.get(key).size(); i++) {
        //         Object preValue = data.get(key).get(i).value;

        //         try {
        //             switch (data.get(key).get(i).type) {
        //                 case DOUBLE:
                            
        //                     break;
        //                 case INT:
                            
        //                     break;
        //                 case STRING:
                            
        //                     break;
        //                 case BOOLEAN:
                            
        //                     break;
        //                 case BYTE_ARRAY:
                            
        //                     break;
        //                 case DOUBLE_ARRAY:
                            
        //                     break;
        //                 case LONG_ARRAY:
                            
        //                     break;
        //                 case STRING_ARRAY:
                            
        //                     break;
        //                 case BOOLEAN_ARRAY:
                            
        //                     break;
        //                 case NONE:
        //                 default:
        //                     break;
        //             }
        //         } catch (Exception e) {
        //             throw new IllegalArgumentException("[IOManager] There was an issue parsing the value returned from the read method " + data.get(key).get(i).name + ". Are the types matched up?");
        //         }
        //     }
        // }

        byte[] log = new byte[1];

        // int i = 0;
        // for (Byte b : prelog) {
        //     log[i] = b;
        //     i++;
        // }

        log[0] = (byte) 0;

        return log;
    }

    public void exit() {
        for (IOLayer layer : this.ioLayers) {
            layer.close();
        }
    }

    private class IOInfo {
        public String name;
        public Object value;
        public IOValue type;
        public boolean real;
        public IOPermission perms;

        public IOInfo(String name, Object value, IOValue type, IOPermission perms, boolean real) {
            this.value = value;
            this.type = type;
            this.name = name;
            this.perms = perms;
            this.real = real;
        }
    }

    private class IOWriteInfo {
        public String name;
        public Method method;
        public IOValue type;
        public IOLayer layer;

        public IOWriteInfo(String name, IOLayer layer, Method method, IOValue type) {
            this.name = name;
            this.method = method;
            this.type = type;
            this.layer = layer;
        }
    }

    public String getBaseName() {
        return "IOManager";
    }
}
