package de.fhkl.imst.i.cgma.raytracer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import de.fhkl.imst.i.cgma.raytracer.file.I_Sphere;
import de.fhkl.imst.i.cgma.raytracer.file.RTFile;
import de.fhkl.imst.i.cgma.raytracer.file.RTFileReader;
import de.fhkl.imst.i.cgma.raytracer.file.RT_Object;
import de.fhkl.imst.i.cgma.raytracer.file.T_Mesh;
import de.fhkl.imst.i.cgma.raytracer.gui.IRayTracerImplementation;
import de.fhkl.imst.i.cgma.raytracer.gui.RayTracerGui;
import de.fhkl.imst.i.cgma.raytracer.gui.RayTracerStatusBar;

public class Raytracer06 implements IRayTracerImplementation {

    public static Raytracer06 INSTANCE = new Raytracer06();
    // viewing volume with infinite end
    private float fovyDegree;
    private float near;
    private float fovyRadians;

    // one hardcoded point light as a minimal solution :-(
    private float[] Ia = { 0.25f, 0.25f, 0.25f }; // ambient light color
    private float[] Ids = { 1.0f, 1.0f, 1.0f }; // diffuse and specular light color
    private float[] ICenter = { 4.0f, 4.0f, 2.0f }; // center of point light

    public RayTracerGui gui = new RayTracerGui(this);

    private int resx, resy; // viewport resolution
    private float h, w, aspect; // window height, width and aspect ratio

    Vector<RT_Object> objects;

    private String datString = "";
    private boolean existServer;
    Connection con = null;
    public boolean client;
    private int clientXP;
    int countClient;
    boolean xpSet = false;
    int xpSetValue;
    List<Integer> intList = new ArrayList<Integer>();
    String datTmp;
    boolean status;
    boolean terminate;
    List<ColumnThreadWrapper> ctw;
    List<Integer> countCTW = new ArrayList<Integer>();

    public Raytracer06() {
        try {
            con = new Connection();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        client = con.getClient();
        try {
            String directory = System.getProperty("user.dir");
            gui.addObject(RTFileReader.read(I_Sphere.class, new File(directory + "/data/ikugel2.dat")));
            gui.addObject(RTFileReader.read(T_Mesh.class, new File(directory + "/data/dreiecke2.dat")));
            gui.addObject(RTFileReader.read(T_Mesh.class, new File(directory + "/data/kugel1.dat")));
            gui.addObject(RTFileReader.read(T_Mesh.class, new File(directory + "/data/kugel2.dat")));
            gui.addObject(RTFileReader.read(T_Mesh.class, new File(directory + "/data/kugel3.dat")));

            objects = gui.getObjects();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setViewParameters(float fovyDegree, float near) {

        // set attributes fovyDegree, fovyRadians, near
        this.near = near;
        this.fovyDegree = fovyDegree;
        fovyRadians = 90.0f;

        // set attributes resx, resy, aspect
        resx = gui.getResX();
        resy = gui.getResY();
        aspect = (float) resx / (float) resy;

        // set attributes h, w
        double tmp = Math.toRadians(fovyDegree);
        h = 2 * near * (float) Math.tan(tmp / 2);
        w = h * aspect;
    }

    @Override
    public void doRayTrace() {
        if (client == false) {
            System.out.println("0 Server");
            try {
                String directory = System.getProperty("user.dir");
                BufferedReader in = new BufferedReader(new FileReader(directory + "/data/dreiecke2.dat"));
                String zeile = null;
                while ((zeile = in.readLine()) != null) {
                    zeile = zeile + "\n";
                    datString = datString + zeile;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            float x, y, z; // intersection point in viewing plane
            float rayEx, rayEy, rayEz; // eye point==ray starting point
            float rayVx, rayVy, rayVz; // ray vector
            Color color;

            // prepare mesh data (normals and areas)
            prepareMeshData();

            // hardcoded viewing volume with fovy and near
            setViewParameters(90.0f, 1.0f);
            // set eye point
            rayEx = rayEy = rayEz = 0.0f;

            z = -near;

            // prepare mesh data for shading
            precalculateMeshDataShading();

            Random rd = new Random();
            // xp, yp: pixel coordinates
            countClient = con.getServerThread().size();
            getDat();
            int clientMax = countClient;

            Iterator<ServerThread> it = con.serverThreadList.iterator();
            while (it.hasNext()) {
                ServerThread st = it.next();
                st.clearList();
            }

            int clientTmp = 1;
            int tmp = 0;
            int oldXp = 0;
            boolean itHasNext = false;

            for (int xp = 0; xp < resx; xp++) {
                intList.add(xp);
            }

            for (int xp = 0; xp < resx; ++xp) {
                if (countClient > clientMax) {
                    countClient = clientMax;
                }
                if (xp == resx - 1) {
                    Boolean isReady = false;
                    while (isReady == false) {
                        Iterator<ServerThread> ite = con.serverThreadList.iterator();
                        while (ite.hasNext()) {
                            ServerThread st = ite.next();
                            if (st.isTerminate() == true) {
                                ite.remove();
                            }
                        }
                        if (con.serverThreadList.size() == 0) {
                            break;
                        }
                        Iterator<ServerThread> its = con.serverThreadList.iterator();
                        while (its.hasNext()) {
                            ServerThread st = its.next();
                            if (st.getCtw().size() == 0) {
                                isReady = true;
                            } else {
                                isReady = false;
                                break;
                            }
                        }
                    }
                    oldXp = xp;
                    Iterator<Integer> i = intList.iterator();
                    if (i.hasNext()) {
                        itHasNext = true;
                        xp = i.next();
                    }
                }
                if (countClient == 0) {
                    for (int yp = 0; yp < resy; ++yp) {
                        // for demo purposes
                        gui.setPixel(xp, yp, Color.BLACK.getRGB());
                        //int xp = 0;
                        //int yp = 0;   

                        // x, y: view coordinates
                        x = ((float) xp / (resx - 1)) * w - (w / 2.0f);
                        y = ((float) (resy - 1 - yp) / (float) (resy - 1)) * h - (h / 2.0f);

                        // ray vector
                        rayVx = x - rayEx;
                        rayVy = y - rayEy;
                        rayVz = z - rayEz;

                        // get color or null along the ray
                        color = traceRayAndGetColor(rayEx, rayEy, rayEz, rayVx, rayVy, rayVz);
                        if (color != null) { //set pixel with color
                            gui.setPixel(xp, yp, color.getRGB());
                        }
                    }
                    removeValueFromIntList(xp);
                } else if (countClient == 1) {
                    switch (tmp) {
                    case 0:
                        for (int yp = 0; yp < resy; ++yp) {
                            // for demo purposes
                            gui.setPixel(xp, yp, Color.BLACK.getRGB());
                            //int xp = 0;
                            //int yp = 0;   

                            // x, y: view coordinates
                            x = ((float) xp / (resx - 1)) * w - (w / 2.0f);
                            y = ((float) (resy - 1 - yp) / (float) (resy - 1)) * h - (h / 2.0f);

                            // ray vector
                            rayVx = x - rayEx;
                            rayVy = y - rayEy;
                            rayVz = z - rayEz;

                            // get color or null along the ray
                            color = traceRayAndGetColor(rayEx, rayEy, rayEz, rayVx, rayVy, rayVz);
                            if (color != null) { //set pixel with color
                                gui.setPixel(xp, yp, color.getRGB());
                            }
                        }
                        removeValueFromIntList(xp);
                        tmp++;
                        break;
                    case 1:
                        drawColumnsClient(xp, clientTmp);
                        tmp = 0;
                        break;
                    default:
                        break;
                    }
                } else if (countClient > 1) {
                    if (xp % (countClient + 1) == 0) {

                        for (int yp = 0; yp < resy; ++yp) {
                            // for demo purposes
                            gui.setPixel(xp, yp, Color.BLACK.getRGB());
                            //int xp = 0;
                            //int yp = 0;	

                            // x, y: view coordinates
                            x = ((float) xp / (resx - 1)) * w - (w / 2.0f);
                            y = ((float) (resy - 1 - yp) / (float) (resy - 1)) * h - (h / 2.0f);

                            // ray vector
                            rayVx = x - rayEx;
                            rayVy = y - rayEy;
                            rayVz = z - rayEz;

                            // get color or null along the ray
                            color = traceRayAndGetColor(rayEx, rayEy, rayEz, rayVx, rayVy, rayVz);
                            if (color != null) { //set pixel with color
                                gui.setPixel(xp, yp, color.getRGB());
                            }
                        }
                        removeValueFromIntList(xp);
                    } else {
                        drawColumnsClient(xp, clientTmp);
                        if (clientTmp < countClient) {
                            clientTmp += 1;
                        } else if (clientTmp == countClient) {
                            clientTmp = 1;
                        }
                    }
                }
                if (itHasNext == true) {
                    xp = oldXp - 1;
                    itHasNext = false;
                }
            }
        }
    }

    float x, y, z; // intersection point in viewing plane
    float rayEx, rayEy, rayEz; // eye point==ray starting point
    float rayVx, rayVy, rayVz; // ray vector
    Color color;

    public void removeValueFromIntList(int xpValue) {
        try {
            Iterator<Integer> it = intList.iterator();
            while (it.hasNext()) {
                int xp = it.next();
                if (xp == xpValue) {
                    it.remove();
                }
            }
        } catch (Exception e) {
            System.out.println("RemoveValueFromList");
        }
    }

    public void drawColumnsClient(final int xp, final int clientTmp) {
        try {
            if (con.serverThreadList.get(clientTmp - 1).isTerminate() == false) {
                con.serverThreadList.get(clientTmp - 1).addToList(con, xp, datString, clientTmp, countClient);
            } else {
                con.serverThreadList.remove(clientTmp - 1);
                if (countClient >= 1) {
                    --countClient;
                }
                howManyClients();
            }
        } catch (Exception e) {
            System.out.println("drawColumnsClient: " + e);
            System.out.println(clientTmp);
            e.printStackTrace();
        }
    }

    String datSphere = "";
    String datMesh = "";

    public void getDat() {
        RTFile scene;
        I_Sphere sphere;
        T_Mesh mesh;

        for (int objectsNumber = 0; objectsNumber < objects.size(); objectsNumber++) {
            scene = objects.get(objectsNumber);
            // object is an implicit sphere?
            if (scene instanceof I_Sphere) {
                sphere = (I_Sphere) scene;
                datSphere = datSphere + sphere.getFileName()
                        + ":IMPLICIT_SPHERE::# r_a g_a b_a    r_d g_d b_d    r_s g_s b_s   n:";
                for (int i = 0; i < sphere.material.length; i++) {
                    if (i < 2) {
                        datSphere = datSphere + sphere.material[i] + " ";
                    } else if (i == 2) {
                        datSphere = datSphere + sphere.material[i] + "   ";
                    } else if (2 < i && i < 5) {
                        datSphere = datSphere + sphere.material[i] + " ";
                    } else if (i == 5) {
                        datSphere = datSphere + sphere.material[i] + "    ";
                    } else if (i > 5 && i < 8) {
                        datSphere = datSphere + sphere.material[i] + " ";
                    } else if (i == 8) {
                        datSphere = datSphere + sphere.material[i] + "   ";
                    }
                }
                datSphere = datSphere + sphere.materialN + ":";
                datSphere = datSphere + ":# cx cy cz    r:";

                for (int i = 0; i < sphere.center.length; i++) {
                    if (i < 2) {
                        datSphere = datSphere + sphere.center[i] + " ";
                    } else if (i == 2) {
                        datSphere = datSphere + sphere.center[i] + "  ";
                    }
                }
                datSphere = datSphere + sphere.radius + ";";
            } else if (scene instanceof T_Mesh) {
                mesh = (T_Mesh) scene;

                datMesh = datMesh + mesh.getFileName() + ":# Header:TRIANGLE_MESH::# Dateiinformationen:";
                datMesh = datMesh + mesh.verticesMat.length + " " + mesh.triangles.length + " "
                        + mesh.materialsN.length + " " + mesh.fgp;
                datMesh = datMesh
                        + "::# Materialien:#  ambient      diffuse        specular    n:# r_a g_a b_a    r_d g_d b_d    r_s g_s b_s   n:";

                for (int i = 0; i < mesh.materials.length; i++) {
                    for (int j = 0; j < mesh.materials[i].length; j++) {
                        if (j < 2) {
                            datMesh = datMesh + mesh.materials[i][j] + " ";
                        } else if (j == 2) {
                            datMesh = datMesh + mesh.materials[i][j] + "   ";
                        } else if (j > 2 && j < 5) {
                            datMesh = datMesh + mesh.materials[i][j] + " ";
                        } else if (j == 5) {
                            datMesh = datMesh + mesh.materials[i][j] + "    ";
                        } else if (j > 5 && j < 8) {
                            datMesh = datMesh + mesh.materials[i][j] + " ";
                        } else if (j == 8) {
                            datMesh = datMesh + mesh.materials[i][j] + "   ";
                        }
                    }
                    datMesh = datMesh + mesh.materialsN[i] + ":";
                }
                datMesh = datMesh + ":# Vertexdaten:";

                for (int i = 0; i < mesh.verticesMat.length; i++) {
                    for (int j = 0; j < mesh.vertices[i].length; j++) {
                        datMesh = datMesh + mesh.vertices[i][j] + " ";
                    }
                    datMesh = datMesh + mesh.verticesMat[i] + ":";
                }

                datMesh = datMesh + "# Dreiecke:";
                for (int i = 0; i < mesh.triangles.length; i++) {
                    for (int j = 0; j < mesh.triangles[i].length; j++) {
                        if (i == mesh.triangles.length - 1 && j == mesh.triangles[i].length - 1) {
                            datMesh = datMesh + mesh.triangles[i][j];
                        } else {
                            datMesh = datMesh + mesh.triangles[i][j] + " ";
                        }
                    }
                    if (i != mesh.triangles.length - 1) {
                        datMesh = datMesh + ":";
                    } else {
                        datMesh = datMesh + ";";
                    }
                }
            }
        }
        String dat = datSphere + datMesh;

        if (con.serverThreadList.size() > 0) {
            Iterator<ServerThread> it = con.serverThreadList.iterator();
            while (it.hasNext()) {
                ServerThread st = it.next();
                st.sendDatString(dat);
            }
        }
        dat = "";
        datMesh = "";
        datSphere = "";
    }

    public void splitAndLoadDatString(String dat) {
        objects = gui.getObjects();
        objects.removeAllElements();
        gui.setObjects(objects);
        objects = gui.getObjects();
        gui.callSgChangedListeners();

        dat = dat.replaceAll(":", "\n");
        String datSplit[] = dat.split("[;]");
        for (int i = 0; i < datSplit.length; i++) {
            int index = datSplit[i].indexOf("\n");

            //FileName
            String fileName = datSplit[i].substring(0, index);

            //Content
            String content = datSplit[i].substring(index + 1);
            write(fileName, content);
            int newLine = datSplit[i].indexOf("\n", index + 1);
            String header = datSplit[i].substring(index + 1, newLine);
            if (header.equals("# Header")) {
                int nextLine = datSplit[i].indexOf("\n", newLine + 1);
                header = datSplit[i].substring(newLine + 1, nextLine);
            }
            try {
                String directory = System.getProperty("user.dir");
                if (header.equals("IMPLICIT_SPHERE")) {
                    gui.addObject(RTFileReader.read(I_Sphere.class, new File(directory + "/data/" + fileName)));
                } else if (header.equals("TRIANGLE_MESH")) {
                    gui.addObject(RTFileReader.read(T_Mesh.class, new File(directory + "/data/" + fileName)));
                }
            } catch (IOException e) {
                System.out.println("addObjects in splitAndLoadDatString " + e);
            }
        }
        objects = gui.getObjects();
    }

    FileWriter writer;
    File file;

    public void write(String fileName, String content) {
        String directory = System.getProperty("user.dir");
        file = new File(directory + "/data/" + fileName);
        try {
            writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int listCounter = 0;

    public void initDoRayTrace() {

        System.out.println("initDoRayTrace");
        // prepare mesh data (normals and areas)
        prepareMeshData();

        // hardcoded viewing volume with fovy and near
        setViewParameters(90.0f, 1.0f);
        // set eye point
        rayEx = rayEy = rayEz = 0.0f;

        z = -near;

        // prepare mesh data for shading
        precalculateMeshDataShading();

        Random rd = new Random();
    }

    public String doRayTrace(int xp) {
        String send = "";
        for (int yp = 0; yp < resy; ++yp) {

            // x, y: view coordinates
            x = ((float) xp / (resx - 1)) * w - (w / 2.0f);
            y = ((float) (resy - 1 - yp) / (float) (resy - 1)) * h - (h / 2.0f);

            // ray vector
            rayVx = x - rayEx;
            rayVy = y - rayEy;
            rayVz = z - rayEz;

            // get color or null along the ray
            color = traceRayAndGetColor(rayEx, rayEy, rayEz, rayVx, rayVy, rayVz);
            if (color != null) { //set pixel with color
                send += (xp + "," + yp + "," + color.getRGB() + ";");
            } else {
                send += (xp + "," + yp + "," + "null" + ";");
            }
        }
        return send;
    }

    // returns Color object or null if no intersection was found
    private Color traceRayAndGetColor(float rayEx, float rayEy, float rayEz, float rayVx, float rayVy, float rayVz) {
        // RTFile scene = gui.getFile();

        double minT = Float.MAX_VALUE;
        int minObjectsIndex = -1;
        int minIndex = -1;

        float[] minIP = new float[3];
        float[] minN = new float[3];
        float[] minMaterial = new float[3];
        float minMaterialN = 1;
        float bu = 0, bv = 0, bw = 1;

        float[] v = new float[3];
        float[] l = new float[3];

        // viewing vector at intersection point
        v[0] = -rayVx;
        v[1] = -rayVy;
        v[2] = -rayVz;
        normalize(v);

        RTFile scene;
        I_Sphere sphere;
        T_Mesh mesh;

        // loop over all scene objects to find the nearest intersection, that
        // is:
        // object with number minObjectIndex
        // minT is the minimal factor t of the ray equation s(t)=rayE+t*rayV
        // where the nearest intersection takes place
        for (int objectsNumber = 0; objectsNumber < objects.size(); objectsNumber++) {
            scene = objects.get(objectsNumber);

            // object is an implicit sphere?
            if (scene instanceof I_Sphere) {
                sphere = (I_Sphere) scene;

                float t;

                // no bounding box hit? -> next object
                if (!bboxHit(sphere, rayEx, rayEy, rayEz, rayVx, rayVy, rayVz))
                    continue;

                // ray intersection uses quadratic equation
                float a, b, c, d;
                a = rayVx * rayVx + rayVy * rayVy + rayVz * rayVz;
                b = 2 * (rayVx * (rayEx - sphere.center[0]) + rayVy * (rayEy - sphere.center[1]) + rayVz
                        * (rayEz - sphere.center[2]));
                c = ((rayEx - sphere.center[0]) * (rayEx - sphere.center[0]))
                        + ((rayEy - sphere.center[1]) * (rayEy - sphere.center[1]))
                        + ((rayEz - sphere.center[2]) * (rayEz - sphere.center[2])) - sphere.radius * sphere.radius;

                // positive discriminant determines intersection
                d = (b * b - 4 * a * c);
                // no intersection point? => next object
                if (d <= 0)
                    continue;

                // from here: intersection takes place!

                // calculate first intersection point with sphere along the
                // ray
                t = 0;

                t = (-b - (float) Math.sqrt(d)) / (2 * a);

                // already a closer intersection point? => next object
                if (t >= minT)
                    continue;

                // from here: t < minT
                // I'm the winner until now!

                minT = t;
                minObjectsIndex = objectsNumber;

                // prepare everything for phong shading

                // the intersection point
                minIP[0] = rayEx + t * rayVx;
                minIP[1] = rayEy + t * rayVy;
                minIP[2] = rayEz + t * rayVz;

                // the normal vector at the intersection point
                minN[0] = minIP[0] - sphere.center[0];
                minN[1] = minIP[1] - sphere.center[1];
                minN[2] = minIP[2] - sphere.center[2];
                normalize(minN);

                // the material
                minMaterial = sphere.material;
                minMaterialN = sphere.materialN;

            }

            // object is a triangle mesh?
            else if (scene instanceof T_Mesh) {
                mesh = (T_Mesh) scene;

                float t;
                float[] n;
                float[] ip = new float[3];

                // no bounding box hit? -> next object
                if (!bboxHit(mesh, rayEx, rayEy, rayEz, rayVx, rayVy, rayVz))
                    continue;

                float a, rayVn, pen;
                float[] p1, p2, p3;
                float[] ai = new float[3];

                // loop over all triangles
                for (int i = 0; i < mesh.triangles.length; i++) {
                    // get the three vertices
                    p1 = mesh.vertices[mesh.triangles[i][0]];
                    p2 = mesh.vertices[mesh.triangles[i][1]];
                    p3 = mesh.vertices[mesh.triangles[i][2]];

                    // intermediate version
                    // calculate normal n and triangle area a
                    //					n = new float[3];
                    //					a = calculateN(n, p1, p2, p3);

                    // fetch precalculated face areas and face normals
                    a = mesh.triangleAreas[i];
                    n = mesh.triangleNormals[i];

                    //Skalarprodukt von rayV und n
                    rayVn = rayVx * n[0] + rayVy * n[1] + rayVz * n[2];

                    // backface? => next triangle
                    if (rayVn >= 0)
                        continue;

                    // no intersection point? => next triangle
                    if (Math.abs(rayVn) < 1E-7)
                        continue;

                    pen = (p1[0] - rayEx) * n[0] + (p1[1] - rayEy) * n[1] + (p1[2] - rayEz) * n[2];

                    // calculate intersection point with plane along the ray
                    t = pen / rayVn;

                    // already a closer intersection point? => next triangle
                    if (t >= minT)
                        continue;

                    // the intersection point with the plane
                    ip[0] = rayEx + t * rayVx;
                    ip[1] = rayEy + t * rayVy;
                    ip[2] = rayEz + t * rayVz;

                    // no intersection point with the triangle? => next
                    // triangle
                    if (!triangleTest(ip, p1, p2, p3, a, ai))
                        continue;

                    // from here: t < minT and triangle intersection
                    // I'm the winner until now!

                    minT = t;
                    minObjectsIndex = objectsNumber;
                    minIndex = i;

                    // prepare everything for shading alternatives

                    // the intersection point
                    minIP[0] = ip[0];
                    minIP[1] = ip[1];
                    minIP[2] = ip[2];

                    switch (mesh.fgp) {
                    case 'f':
                    case 'F':

                        //						// the normal is the surface normal 
                        //						minN[0] = n[0];
                        //						minN[1] = n[1]; 
                        //						minN[2] = n[2];
                        //
                        //						// the material is the material of the first triangle point 
                        //						int matIndex = mesh.verticesMat[mesh.triangles[minIndex][0]];
                        //						minMaterial = mesh.materials[matIndex]; 
                        //						minMaterialN= mesh.materialsN[matIndex];

                        break;
                    case 'g':
                    case 'G':
                        // remember barycentric coordinates bu, bv, bw for shading
                        bu = ai[0] / a;
                        bv = ai[1] / a;
                        bw = ai[2] / a;

                        break;
                    case 'p':
                    case 'P':
                        // the normal is barycentrically interpolated between
                        // the three vertices
                        bu = ai[0] / a;
                        bv = ai[1] / a;
                        bw = ai[2] / a;

                        float nTemp[] = new float[3];
                        nTemp[0] = mesh.vertexNormals[mesh.triangles[minIndex][0]][0] * bu
                                + mesh.vertexNormals[mesh.triangles[minIndex][1]][0] * bv
                                + mesh.vertexNormals[mesh.triangles[minIndex][2]][0] * bw;
                        nTemp[1] = mesh.vertexNormals[mesh.triangles[minIndex][0]][1] * bu
                                + mesh.vertexNormals[mesh.triangles[minIndex][1]][1] * bv
                                + mesh.vertexNormals[mesh.triangles[minIndex][2]][1] * bw;
                        nTemp[2] = mesh.vertexNormals[mesh.triangles[minIndex][0]][2] * bu
                                + mesh.vertexNormals[mesh.triangles[minIndex][1]][2] * bv
                                + mesh.vertexNormals[mesh.triangles[minIndex][2]][2] * bw;

                        minN = nTemp;

                        // intermediate version
                        // the material is not interpolated
                        // matIndex =
                        // mesh.verticesMat[mesh.triangles[minIndex][0]];
                        // minMaterial = mesh.materials[matIndex];
                        // minMaterialN = mesh.materialsN[matIndex];

                        // the material is barycentrically interpolated between
                        // the three vertex materials
                        int matIndex0 = mesh.verticesMat[mesh.triangles[minIndex][0]];
                        int matIndex1 = mesh.verticesMat[mesh.triangles[minIndex][1]];
                        int matIndex2 = mesh.verticesMat[mesh.triangles[minIndex][2]];
                        float materialTemp[] = new float[9];
                        float materialNTemp;
                        for (int k = 0; k < 9; k++) {
                            materialTemp[k] = mesh.materials[matIndex0][k] * bu + mesh.materials[matIndex1][k] * bv
                                    + mesh.materials[matIndex2][k] * bw;
                        }
                        minMaterial = materialTemp;
                        materialNTemp = bu * mesh.materialsN[matIndex0] + bv * mesh.materialsN[matIndex1] + bw
                                * mesh.materialsN[matIndex2];
                        minMaterialN = materialNTemp;
                    }
                }
            } else
                continue; // return null;

        }

        // no intersection point found => return with no result
        if (minObjectsIndex == -1)
            return null;

        // light vector at the intersection point
        l[0] = ICenter[0] - minIP[0];
        l[1] = ICenter[1] - minIP[1];
        l[2] = ICenter[2] - minIP[2];
        normalize(l);

        // decide which shading model will be applied

        // implicit: only phong shading available => shade=illuminate
        if (objects.get(minObjectsIndex) instanceof I_Sphere)
            return phongIlluminate(minMaterial, minMaterialN, l, minN, v, Ia, Ids);

        // triangle mesh: flat, gouraud or phong shading according to file data
        else if (objects.get(minObjectsIndex).getHeader() == "TRIANGLE_MESH") {
            mesh = ((T_Mesh) objects.get(minObjectsIndex));
            switch (mesh.fgp) {
            case 'f':
            case 'F':
                // illumination can be calculated here
                // this is a variant between flat und phong shading
                //				return phongIlluminate(minMaterial, minMaterialN, l, minN, v, Ia, Ids);

                // lookup triangle color of triangle hit
                return new Color(mesh.triangleColors[minIndex][0] > 1 ? 1 : mesh.triangleColors[minIndex][0],
                        mesh.triangleColors[minIndex][1] > 1 ? 1 : mesh.triangleColors[minIndex][1],
                        mesh.triangleColors[minIndex][2] > 1 ? 1.0f : mesh.triangleColors[minIndex][2]);
            case 'g':
            case 'G':
                // the color is barycentrically interpolated between the three
                // vertex colors
                float colorf[] = new float[3];
                colorf[0] = bu * mesh.vertexColors[mesh.triangles[minIndex][0]][0] + bv
                        * mesh.vertexColors[mesh.triangles[minIndex][1]][0] + bw
                        * mesh.vertexColors[mesh.triangles[minIndex][2]][0];
                colorf[1] = bu * mesh.vertexColors[mesh.triangles[minIndex][0]][1] + bv
                        * mesh.vertexColors[mesh.triangles[minIndex][1]][1] + bw
                        * mesh.vertexColors[mesh.triangles[minIndex][2]][1];
                colorf[2] = bu * mesh.vertexColors[mesh.triangles[minIndex][0]][2] + bv
                        * mesh.vertexColors[mesh.triangles[minIndex][1]][2] + bw
                        * mesh.vertexColors[mesh.triangles[minIndex][2]][2];

                return new Color(colorf[0] > 1 ? 1 : colorf[0], colorf[1] > 1 ? 1 : colorf[1], colorf[2] > 1 ? 1.0f
                        : colorf[2]);
            case 'p':
            case 'P':
                // calculate the color per per pixel phong lightning
                return phongIlluminate(minMaterial, minMaterialN, l, minN, v, Ia, Ids);
                // return new Color(material[3], material[4], material[5]);
                // break;
            }
        }

        return null;
        //		// intermediate version
        //		Random rd = new Random();
        //		return new Color(rd.nextFloat(), rd.nextFloat(), rd.nextFloat());

    }

    // calculate phong illumination model with material parameters material and
    // materialN, light vector l, normal vector n, viewing vector v, ambient
    // light Ia, diffuse and specular light Ids
    // return value is a new Color object
    private Color phongIlluminate(float[] material, float materialN, float[] l, float[] n, float[] v, float[] Ia,
            float[] Ids) {
        float ir = 0, ig = 0, ib = 0; // reflected intensity, rgb channels
        float[] r = new float[3]; // reflection vector
        float ln, rv; // scalar products <l,n> and <r,v>

        // <l,n>
        ln = l[0] * n[0] + l[1] * n[1] + l[2] * n[2];

        // ambient component, Ia*ra
        ir += Ia[0] * material[0];
        ig += Ia[1] * material[1];
        ib += Ia[2] * material[2];

        // diffuse component, Ids*rd*<l,n>
        if (ln > 0) {
            ir += Ids[0] * material[0 + 3] * ln;
            ig += Ids[1] * material[1 + 3] * ln;
            ib += Ids[2] * material[2 + 3] * ln;

            // reflection vector r=2*<l,n>*n-l
            r[0] = 2 * ln * n[0] - l[0];
            r[1] = 2 * ln * n[1] - l[1];
            r[2] = 2 * ln * n[2] - l[2];
            normalize(r);

            // <r,v>
            rv = r[0] * v[0] + r[1] * v[1] + r[2] * v[2];

            // specular component, Ids*rs*<r,v>^n
            if (rv > 0) {
                float pow = (float) Math.pow(rv, materialN);
                ir += Ids[0] * material[0 + 6] * pow;
                ig += Ids[1] * material[1 + 6] * pow;
                ib += Ids[2] * material[2 + 6] * pow;
            }
        }

        return new Color(ir > 1 ? 1 : ir, ig > 1 ? 1 : ig, ib > 1 ? 1 : ib);
    }

    // vector normalization
    // CAUTION: vec is an in-/output parameter; the referenced object will be
    // altered!
    private float normalize(float[] vec) {
        float l;

        l = (float) Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);

        vec[0] /= l;
        vec[1] /= l;
        vec[2] /= l;

        return l;
    }

    // calculate normalized face normal fn of the triangle p1, p2 and p3
    // the return value is the area of triangle
    // CAUTION: fn is an output parameter; the referenced object will be
    // altered!
    private float calculateN(float[] fn, float[] p1, float[] p2, float[] p3) {
        float ax, ay, az, bx, by, bz;

        // a = Vi2-Vi1, b = Vi3-Vi1
        ax = p2[0] - p1[0];
        ay = p2[1] - p1[1];
        az = p2[2] - p1[2];

        bx = p3[0] - p1[0];
        by = p3[1] - p1[1];
        bz = p3[2] - p1[2];

        // n = a x b

        fn[0] = ay * bz - az * by;
        fn[1] = -ax * bz + az * bx;
        fn[2] = ax * by - ay * bx;

        // normalize n, calculate and return area of triangle
        return normalize(fn) / 2;
    }

    // calculate triangle test
    // is p (the intersection point with the plane through p1, p2 and p3) inside
    // the triangle p1, p2 and p3?
    // the return value answers this question
    // a is an input parameter - the given area of the triangle p1, p2 and p3
    // ai will be computed to be the areas of the sub-triangles to allow to
    // compute barycentric coordinates of the intersection point p
    // ai[0] is associated with bu (p1p2p) across from p3
    // ai[1] is associated with bv (pp2p3) across from p1
    // ai[2] is associated with bw (p1pp3) across form p2
    // CAUTION: ai is an output parameter; the referenced object will be
    // altered!
    private boolean triangleTest(float[] p, float[] p1, float[] p2, float[] p3, float a, float ai[]) {
        float tmp[] = new float[3];

        ai[0] = calculateN(tmp, p1, p2, p);
        ai[1] = calculateN(tmp, p, p2, p3);
        ai[2] = calculateN(tmp, p1, p, p3);

        float epsilon = (float) Math.pow(10.0, -5);

        if (Math.abs(ai[0] + ai[1] + ai[2] - a) <= epsilon)
            return true;

        return false;
    }

    // calculate bounding box test
    // decides whether the ray s(t)=rayE+t*rayV intersects the axis aligned
    // bounding box of object -> return value true
    // six plane intersections with rectangle inside tests; if one succeeds
    // bounding box is hit
    private boolean bboxHit(RT_Object object, float rayEx, float rayEy, float rayEz, float rayVx, float rayVy,
            float rayVz) {
        float t;
        float ip[] = new float[3];

        // front and back
        if (Math.abs(rayVz) > 1E-5) {
            // front xy
            t = (object.max[2] - rayEz) / rayVz;

            ip[0] = rayEx + t * rayVx;
            ip[1] = rayEy + t * rayVy;

            if (ip[0] > object.min[0] && ip[0] < object.max[0] && ip[1] > object.min[1] && ip[1] < object.max[1])
                return true;

            // back xy
            t = (object.min[2] - rayEz) / rayVz;

            ip[0] = rayEx + t * rayVx;
            ip[1] = rayEy + t * rayVy;

            if (ip[0] > object.min[0] && ip[0] < object.max[0] && ip[1] > object.min[1] && ip[1] < object.max[1])
                return true;
        }

        // left and right
        if (Math.abs(rayVx) > 1E-5) {

            //right
            t = (object.max[0] - rayEx) / rayVx;

            ip[1] = rayEy + t * rayVy;
            ip[2] = rayEz + t * rayVz;

            if (ip[1] > object.min[1] && ip[1] < object.max[1] && ip[2] > object.min[2] && ip[2] < object.max[2])
                return true;

            //left
            t = (object.min[0] - rayEx) / rayVx;

            ip[1] = rayEy + t * rayVy;
            ip[2] = rayEz + t * rayVz;

            if (ip[1] > object.min[1] && ip[1] < object.max[1] && ip[2] > object.min[2] && ip[2] < object.max[2])
                return true;

        }
        // top and bottom
        if (Math.abs(rayVy) > 1E-5) {

            //TOP
            t = (object.max[1] - rayEy) / rayVy;

            ip[0] = rayEx + t * rayVx;
            ip[2] = rayEz + t * rayVz;

            if (ip[0] > object.min[0] && ip[0] < object.max[0] && ip[2] > object.min[2] && ip[2] < object.max[2])
                return true;

            //bottom
            t = (object.min[1] - rayEy) / rayVy;

            ip[0] = rayEx + t * rayVx;
            ip[2] = rayEz + t * rayVz;

            if (ip[0] > object.min[0] && ip[0] < object.max[0] && ip[2] > object.min[2] && ip[2] < object.max[2])
                return true;
        }
        return false;
    }

    // precalulation of triangle normals and triangle areas
    private void prepareMeshData() {
        RTFile scene;

        System.out.println("Vorverarbeitung 1 lŠuft");

        float[] p1, p2, p3;

        for (int objectsNumber = 0; objectsNumber < objects.size(); objectsNumber++) {
            scene = objects.get(objectsNumber);
            if (scene.getHeader() == "TRIANGLE_MESH") {
                T_Mesh mesh = (T_Mesh) scene;

                // init memory
                mesh.triangleNormals = new float[mesh.triangles.length][3];
                mesh.triangleAreas = new float[mesh.triangles.length];

                for (int i = 0; i < mesh.triangles.length; i++) {
                    p1 = mesh.vertices[mesh.triangles[i][0]];
                    p2 = mesh.vertices[mesh.triangles[i][1]];
                    p3 = mesh.vertices[mesh.triangles[i][2]];

                    // calculate and store triangle normal n and triangle area a
                    mesh.triangleAreas[i] = calculateN(mesh.triangleNormals[i], p1, p2, p3);
                }
            }
        }
        System.out.println("Vorverarbeitung 1 beendet");

    }

    // view dependend precalculation dependend on type of mesh shading
    // vertexNormals for phong and gouraud shading
    // vertexColors for gouraud shading
    // triangleColors for flat lighting
    private void precalculateMeshDataShading() {
        RTFile scene;

        System.out.println("Vorverarbeitung 2 lŠuft");

        float rayEx, rayEy, rayEz, rayVx, rayVy, rayVz;
        double rayVn;
        Color color;
        float x, y, z;
        float[] ip = new float[3];
        float[] n = new float[3];
        float[] l = new float[3];
        float[] v = new float[3];
        float[] material;
        float materialN;
        int matIndex;

        for (int objectsNumber = 0; objectsNumber < objects.size(); objectsNumber++) {
            scene = objects.get(objectsNumber);

            if (scene.getHeader() == "TRIANGLE_MESH") {
                T_Mesh mesh = (T_Mesh) scene;

                switch (mesh.fgp) {
                case 'f':
                case 'F':
                    // for flat-shading: initialize and calculate triangle
                    // colors
                    mesh.triangleColors = new float[mesh.triangles.length][3];

                    rayEx = 0.0f;
                    rayEy = 0.0f;
                    rayEz = 0.0f;

                    // loop over all triangles
                    for (int i = 0; i < mesh.triangles.length; i++) {
                        // the intersection point is the first vertex of the
                        // triangle
                        ip = mesh.vertices[mesh.triangles[i][0]];

                        // the material is the material of the first triangle
                        // point
                        matIndex = mesh.verticesMat[mesh.triangles[i][0]];
                        material = mesh.materials[matIndex];
                        materialN = mesh.materialsN[matIndex];

                        // x, y, z: view coordinates are intersection point
                        x = ip[0];
                        y = ip[1];
                        z = ip[2];

                        // ray vector
                        rayVx = x - rayEx;
                        rayVy = y - rayEy;
                        rayVz = z - rayEz;

                        // fetch precalculated face normal
                        n = mesh.triangleNormals[i];

                        rayVn = rayVx * n[0] + rayVy * n[1] + rayVz * n[2];

                        // backface? => next triangle
                        if (rayVn >= 0)
                            continue;

                        // light vector at the intersection point
                        l[0] = ICenter[0] - ip[0];
                        l[1] = ICenter[1] - ip[1];
                        l[2] = ICenter[2] - ip[2];
                        normalize(l);

                        // viewing vector at intersection point
                        v[0] = -rayVx;
                        v[1] = -rayVy;
                        v[2] = -rayVz;
                        normalize(v);

                        // illuminate
                        color = phongIlluminate(material, materialN, l, n, v, Ia, Ids);

                        // write color to triangle
                        mesh.triangleColors[i] = color.getColorComponents(null);
                        // mesh.triangleColors[i][1] =
                        // mesh.triangleColors[i][2] =
                    }
                    break;

                case 'p':
                case 'P':
                case 'g':
                case 'G':
                    // initialize and calculate averaged vertex normals
                    mesh.vertexNormals = new float[mesh.vertices.length][3];

                    // loop over all vertices to initialize
                    for (int j = 0; j < mesh.vertices.length; j++)
                        for (int k = 0; k < 3; k++)
                            mesh.vertexNormals[j][k] = 0.0f;

                    // loop over all faces to contribute
                    for (int i = 0; i < mesh.triangles.length; i++)
                        for (int j = 0; j < 3; j++)
                            for (int k = 0; k < 3; k++)
                                mesh.vertexNormals[mesh.triangles[i][j]][k] += mesh.triangleNormals[i][k];
                    // loop over all vertices to normalize
                    for (int j = 0; j < mesh.vertices.length; j++) {
                        normalize(mesh.vertexNormals[j]);
                    }

                    // these are all preparations for phong shading
                    if (mesh.fgp == 'p' || mesh.fgp == 'P')
                        break;

                    // for gouraud-shading: initialize and calculate vertex
                    // colors
                    mesh.vertexColors = new float[mesh.vertices.length][3];

                    rayEx = 0.0f;
                    rayEy = 0.0f;
                    rayEz = 0.0f;

                    // loop over all vertices
                    for (int i = 0; i < mesh.vertices.length; i++) {
                        // the intersection point is the vertex
                        ip = mesh.vertices[i];

                        // the material is the material of the vertex
                        matIndex = mesh.verticesMat[i];
                        material = mesh.materials[matIndex];
                        materialN = mesh.materialsN[matIndex];

                        // x, y, z: view coordinates are intersection point
                        x = ip[0];
                        y = ip[1];
                        z = ip[2];

                        // ray vector
                        rayVx = x - rayEx;
                        rayVy = y - rayEy;
                        rayVz = z - rayEz;

                        // fetch precalculated vertex normal
                        n = mesh.vertexNormals[i];

                        rayVn = rayVx * n[0] + rayVy * n[1] + rayVz * n[2];

                        // backface? => next vertex
                        if (rayVn >= 0)
                            continue;

                        // light vector at the intersection point
                        l[0] = ICenter[0] - ip[0];
                        l[1] = ICenter[1] - ip[1];
                        l[2] = ICenter[2] - ip[2];
                        normalize(l);

                        // viewing vector at intersection point
                        v[0] = -rayVx;
                        v[1] = -rayVy;
                        v[2] = -rayVz;
                        normalize(v);

                        // illuminate
                        color = phongIlluminate(material, materialN, l, n, v, Ia, Ids);

                        // write color to vertex
                        mesh.vertexColors[i] = color.getColorComponents(null);
                        // mesh.vertexColors[i][1] =
                        // mesh.vertexColors[i][2] =
                    }
                }
            }
        }
        System.out.println("Vorverarbeitung 2 beendet");
    }

    public static void main(String[] args) {

    }

    public boolean isClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public String getDatTmp() {
        return datTmp;
    }

    public void setDatTmp(String datTmp) {
        this.datTmp = datTmp;
    }

    public void howManyClients() {

        int count = con.getServerThread().size();
        if (count == 1) {
            RayTracerStatusBar.setClientLabel(count + " Client connected");
        } else {
            RayTracerStatusBar.setClientLabel(count + " Clients connected");
        }
    }

    public void serverOrClient() {
        System.out.println(client);
        if (client == false) {
            RayTracerStatusBar.setClientOrServer("Server");
        } else {
            RayTracerStatusBar.setClientOrServer("Client");
            objects = gui.getObjects();
            objects.removeAllElements();
            gui.setObjects(objects);
            objects = gui.getObjects();
            gui.callSgChangedListeners();
        }
    }

    int countCTWList = 0;

    public void creatList() {
        countCTWList++;
        ctw = new ArrayList<ColumnThreadWrapper>();
    }
}
