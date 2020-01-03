package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

class PDGAntiDependence extends PDGAbstractDataDependence {

    PDGAntiDependence(PDGNode src, PDGNode dst,
                      AbstractVariable data, CFGBranchNode loop) {
        super(src, dst, PDGDependenceType.ANTI, data, loop);
    }

}
