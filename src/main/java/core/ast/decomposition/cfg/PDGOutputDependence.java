package core.ast.decomposition.cfg;

class PDGOutputDependence extends PDGAbstractDataDependence {

    PDGOutputDependence(PDGNode src, PDGNode dst,
                        AbstractVariable data, CFGBranchNode loop) {
        super(src, dst, PDGDependenceType.OUTPUT, data, loop);
    }

}
