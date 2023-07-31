package frc.team_8840_lib.utils.IO;

import frc.team_8840_lib.IO.IOManager;

public class IOLayer {
    private boolean real = true;
    
    public IOLayer() {
        real = true;

        //Required method.
        IOManager.addIO(this);
    }

    public void setReal(boolean real) {
        this.real = real;
    }

    public boolean isReal() {
        return this.real;
    }

    /*
        These methods are some examples of what reading/and writing can look like.
        These are commented out since IOManager may pick them up, which isn't good.

        <code>
        @IOMethod( name = "default", value_type = IOValue.BYTE_ARRAY, method_type = IOMethodType.READ )
        public Object read() {
            return new byte[0];
        }

        @IOMethod( name = "default", value_type = IOValue.BYTE_ARRAY, method_type = IOMethodType.READ )
        public void write(Object value) {
            byte[] bytes = (byte[]) value; //That's not really possibly in java, but you get the point.
        }
        </code>
    */

    public String getBaseName() {
        return "Empty IOLayer";
    }

    public void close() {
        //Do nothing.
    }
}
