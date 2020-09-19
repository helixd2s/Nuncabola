package com.uppgarn.nuncabola.core.renderers;
import com.uppgarn.nuncabola.core.solid.*;

import com.helixd2s.valera.ValerABase;

public class BodySet {
    public SolidBase solBase;
    public Body body;
    public Mesh meshes[];
    public ValerABase.GeometrySet geometrySet;

    // 
    BodySet(SolidBase solBase, Body body, Mesh meshes[]) {
        this.solBase = solBase;
        this.body = body;
        this.meshes = meshes;
    };

    public void deinitialize() {

    };
};
