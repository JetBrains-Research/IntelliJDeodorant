package core.ast.decomposition.cfg;

class PDGDataDependence extends PDGAbstractDataDependence {

    PDGDataDependence(PDGNode src, PDGNode dst,
                      AbstractVariable data, CFGBranchNode loop) {
        super(src, dst, PDGDependenceType.DATA, data, loop);
    }

}
