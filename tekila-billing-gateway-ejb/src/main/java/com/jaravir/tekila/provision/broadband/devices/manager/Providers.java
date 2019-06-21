package com.jaravir.tekila.provision.broadband.devices.manager;

/**
 * Created by shnovruzov on 6/7/2016.
 */
public enum Providers {

    AZERTELECOM(454100L),
    AZERTELECOMPOST(454110L),
    BBTV(454101L),
    BCC(454102L),
    M2M(454103L),
    CITYNET(454105L),
    QUTU(454106L),
    QUTUNARHOME(454114L),
    BBTV_BAKU(454104L),
    UNINET(454107L),
    NARFIX(454108L),
    DATAPLUS(454111L),
    GLOBAL(454112L),
    CNC(454113L),
    QUTU_AZERTELECOM(454114L);

    private Long id;

    Providers(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public static Providers findById(Long id) {
        for (Providers p : Providers.values()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }
}
