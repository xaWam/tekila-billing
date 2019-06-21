package com.jaravir.tekila.module.system.terminal;

import java.io.Serializable;

/**
 * Created by khsadigov on 7/24/2016.
 */
public abstract class Command implements Serializable {

    public String run() {
        return "Not Implemented";
    }

    public String run(String[] params) {
        return "Not Implemented";
    }

}
