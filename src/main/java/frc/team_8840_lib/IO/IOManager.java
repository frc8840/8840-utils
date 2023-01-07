package frc.team_8840_lib.IO;

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import frc.team_8840_lib.IO.devices.IOPowerDistribution;
import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.console.Logger.LogType;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.logging.Loggable;

public class IOManager implements Loggable {

    private static IOManager instance;

    public static IOManager getInstance() {
        return instance;
    }

    public static void init() {
        instance = new IOManager();

        IOPowerDistribution.init();
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

    @AutoLog( logtype = LogType.BYTE_ARRAY, name = "IO" )
    public byte[] readAndSendIOInformation() {
        HashMap<String, ArrayList<IOInfo>> data = new HashMap<>();

        for (IOLayer layer : this.ioLayers) {
            IOPermission perms = IOPermission.NONE;

            if (layer.getClass().getAnnotation(IOAccess.class) != null) {
                perms = layer.getClass().getAnnotation(IOAccess.class).value();
            } else {
                throw new AnnotationFormatError("[IOManager] Annotation not found on any method in IOLayer class " + layer.getBaseName() + " (class: " + layer.getClass().getName() + ")");
            }

            boolean hasRead = false;
            boolean hasWrite = false;

            for (Method method : layer.getClass().getMethods()) {                
                IOMethod iomethod = method.getAnnotation(IOMethod.class);

                if (iomethod == null) {
                    continue;
                }

                /*
                We don't need the write methods since they're supposed to be invoked
                when writing. All we're doing here is reading the methods so
                it's not needed.
                */
                if (iomethod.method_type() == IOMethodType.WRITE) {
                    hasWrite = true;

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
                                layer.getBaseName() + "/" + iomethod.name(),
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
            if (!(hasRead && hasWrite) && perms == IOPermission.READ_WRITE) {
                throw new IllegalArgumentException("[IOManager] Missing either read or write methods on IO layer " + layer.getBaseName() + "(class name: " + layer.getClass().getName() + ")" + " when marked as having both. Either change the IOPermissions or add in a read or write method.");
            }

            if (!hasRead && perms == IOPermission.READ) {
                throw new IllegalArgumentException("[IOManager] Missing either ");
            }
        }

        if (outputingToComms) {
            for (String _key : data.keySet()) {
                HashMap<String, Integer> nameCalled = new HashMap<>();
                for (int i = 0; i < data.get(_key).size(); i++) {
                    Object preValue = data.get(_key).get(i).value;
                    String key = data.get(_key).get(i).name;

                    if (!nameCalled.keySet().contains(key)) {
                        nameCalled.put(key, 0);
                    }

                    int numCalled = nameCalled.get(key);
                    nameCalled.put(key, numCalled + 1);

                    try {

                        switch (data.get(_key).get(i).type) {
                            case DOUBLE:
                                CommunicationManager.getInstance()
                                    .updateInfo("IO", key + "/" + numCalled + "/value", (double) preValue);
                                    break;
                            case INT:
                                CommunicationManager.getInstance()
                                    .updateInfo("IO", key + "/" + numCalled + "/value", (int) preValue);
                                break;
                            case STRING:
                                CommunicationManager.getInstance()
                                    .updateInfo("IO", key + "/" + numCalled + "/value", (String) preValue);
                                break;
                            case BOOLEAN:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/" + numCalled + "/value", (boolean) preValue);
                                break;
                            case BYTE_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/" + numCalled + "/value", (byte[]) preValue);
                                break;
                            case DOUBLE_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/" + numCalled + "/value", (double[]) preValue);
                                break;
                            case LONG_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/" + numCalled + "/value", (long[]) preValue);
                                break;
                            case STRING_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/" + numCalled + "/value", (String[]) preValue);
                                break;
                            case BOOLEAN_ARRAY:
                                CommunicationManager.getInstance().updateInfo("IO", key + "/" + numCalled + "/value", (boolean[]) preValue);
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
                            key + "/" + numCalled + "/type", //EX: /default/0/type
                            data.get(_key).get(i).type.name()
                        )
                        .updateInfo(
                            "IO", 
                            key + "/" + numCalled + "/p", //EX: /default/0/p
                            data.get(_key).get(i).perms.shortName()
                        )
                        .updateInfo(
                            "IO", 
                            key + "/" + numCalled + "/real", 
                            data.get(_key).get(i).real
                        );
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
}
