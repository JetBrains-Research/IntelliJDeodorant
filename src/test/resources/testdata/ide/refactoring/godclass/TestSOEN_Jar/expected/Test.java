package TestSOEN_Jar.actual;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


class Test {
    private TestProduct testProduct = new TestProduct();
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    protected Project project;

    public Resource[][] grabManifests(ResourceCollection[] rcs) {
        Resource[][] manifests = new Resource[rcs.length][];
        for (int i = 0; i < rcs.length; i++) {
            Resource[][] resources = null;
            if (rcs[i] instanceof FileSet) {
                resources = testProduct.grabResources(new FileSet[]{(FileSet) rcs[i]}, this.project);
            } else {
                resources = grabNonFileSetResources(new ResourceCollection[]{
                        rcs[i]
                });
            }
            for (int j = 0; j < resources[0].length; j++) {
                String name = resources[0][j].getName().replace('\\', '/');
                if (rcs[i] instanceof ArchiveFileSet) {
                    ArchiveFileSet afs = (ArchiveFileSet) rcs[i];
                    if (!"".equals(afs.getFullpath(getProject()))) {
                        name = afs.getFullpath(getProject());
                    } else if (!"".equals(afs.getPrefix(getProject()))) {
                        String prefix = afs.getPrefix(getProject());
                        if (!prefix.endsWith("/") && !prefix.endsWith("\\")) {
                            prefix += "/";
                        }
                        name = prefix + name;
                    }
                }
                if (name.equalsIgnoreCase(MANIFEST_NAME)) {
                    manifests[i] = new Resource[]{resources[0][j]};
                    break;
                }
            }
            if (manifests[i] == null) {
                manifests[i] = new Resource[0];
            }
        }
        return manifests;
    }

    public Project getProject() {
        return project;
    }

    protected Resource[][] grabNonFileSetResources(ResourceCollection[] rcs) {
        Resource[][] result = new Resource[rcs.length][];
        for (int i = 0; i < rcs.length; i++) {
            ArrayList<Resource> dirs = new ArrayList<Resource>();
            ArrayList<Resource> files = new ArrayList<Resource>();
            for (Resource r : rcs[i]) {
                if (r.isExists()) {
                    if (r.isDirectory()) {
                        dirs.add(r);
                    } else {
                        files.add(r);
                    }
                }
            }
            // make sure directories are in alpha-order - this also
            // ensures parents come before their children
            Collections.sort(dirs, new Comparator<Resource>() {
                public int compare(Resource r1, Resource r2) {
                    return r1.getName().compareTo(r2.getName());
                }
            });
            ArrayList<Resource> rs = new ArrayList<Resource>(dirs);
            rs.addAll(files);
            result[i] = rs.toArray(new Resource[rs.size()]);
        }
        return result;
    }

    protected Resource[][] grabResources(FileSet[] filesets) {
        return testProduct.grabResources(filesets, this.project);
    }
}
