package TestSOEN_Jar.actual;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.ZipScanner;

import java.util.Vector;

public class TestProduct {
    private boolean doFilesonly = false;
    private String encoding;

    public Resource[][] grabResources(FileSet[] filesets, Project thisProject) {
        Resource[][] result = new Resource[filesets.length][];
        for (int i = 0; i < filesets.length; i++) {
            boolean skipEmptyNames = true;
            if (filesets[i] instanceof ZipFileSet) {
                ZipFileSet zfs = (ZipFileSet) filesets[i];
                skipEmptyNames = zfs.getPrefix(thisProject).equals("")
                        && zfs.getFullpath(thisProject).equals("");
            }
            DirectoryScanner rs =
                    filesets[i].getDirectoryScanner(thisProject);
            if (rs instanceof ZipScanner) {
                ((ZipScanner) rs).setEncoding(encoding);
            }
            Vector<Resource> resources = new Vector<Resource>();
            if (!doFilesonly) {
                String[] directories = rs.getIncludedDirectories();
                for (int j = 0; j < directories.length; j++) {
                    if (!"".equals(directories[j]) || !skipEmptyNames) {
                        resources.addElement(rs.getResource(directories[j]));
                    }
                }
            }
            String[] files = rs.getIncludedFiles();
            for (int j = 0; j < files.length; j++) {
                if (!"".equals(files[j]) || !skipEmptyNames) {
                    resources.addElement(rs.getResource(files[j]));
                }
            }

            result[i] = new Resource[resources.size()];
            resources.copyInto(result[i]);
        }
        return result;
    }
}