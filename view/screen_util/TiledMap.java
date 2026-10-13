package view.screen_util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelReader;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TiledMap {
    public int width;
    public int height;
    public int tileWidth;
    public int tileHeight;

    public static class Layer {
        public String name;
        public int[][] data;
    }

    public static class TiledObject {
        public int id;
        public String name;
        public String type;
        public int gid;
        public double x, y, width, height, rotation;
        public Map<String, String> properties = new HashMap<>();
    }

    public List<Layer> layers = new ArrayList<>();
    public List<TiledObject> objects = new ArrayList<>();
    public Map<Integer, Image> tiles = new HashMap<>();

    public TiledMap(String tmxPath) {
        loadMap(tmxPath);
    }

    private InputStream getStream(String path) {
        try {
            File f = new File(System.getProperty("user.dir"), path.startsWith("/") ? path.substring(1) : path);
            if (f.exists()) return new FileInputStream(f);
            return getClass().getResourceAsStream(path);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadMap(String tmxPath) {
        System.out.println("=========================================");
        System.out.println("[TiledMapLoader] BẮT ĐẦU ĐỌC MAP TỪ: " + tmxPath);
        try {
            InputStream is = getStream(tmxPath);
            if (is == null) {
                System.err.println("[TiledMapLoader] LỖI NGHIÊM TRỌNG: Không thể đọc file " + tmxPath);
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            Element mapElement = doc.getDocumentElement();
            this.width = Integer.parseInt(mapElement.getAttribute("width"));
            this.height = Integer.parseInt(mapElement.getAttribute("height"));
            this.tileWidth = Integer.parseInt(mapElement.getAttribute("tilewidth"));
            this.tileHeight = Integer.parseInt(mapElement.getAttribute("tileheight"));

            System.out.println("[TiledMapLoader] Thông số map: " + this.width + "x" + this.height + " tiles, kích thước tile: " + this.tileWidth + "x" + this.tileHeight);

            String basePath = tmxPath.substring(0, tmxPath.lastIndexOf("/") + 1);

            // Parse Tilesets
            NodeList tilesetNodes = doc.getElementsByTagName("tileset");
            for (int i = 0; i < tilesetNodes.getLength(); i++) {
                Element tsElement = (Element) tilesetNodes.item(i);
                int firstGid = Integer.parseInt(tsElement.getAttribute("firstgid"));
                
                Element tsRoot = tsElement;
                if (tsElement.hasAttribute("source")) {
                    String source = tsElement.getAttribute("source");
                    // some sources have ../../../, let's just use the basename and assume it's in the same dir for now, or resolve it
                    String tsxName = source.substring(source.lastIndexOf("/") + 1);
                    InputStream tsxIs = getStream(basePath + tsxName);
                    if (tsxIs != null) {
                        Document tsxDoc = builder.parse(tsxIs);
                        tsRoot = tsxDoc.getDocumentElement();
                    }
                }

                // If single spritesheet
                NodeList imageNodes = tsRoot.getElementsByTagName("image");
                if (imageNodes.getLength() == 1 && imageNodes.item(0).getParentNode().getNodeName().equals("tileset")) {
                    Element imgElement = (Element) imageNodes.item(0);
                    String source = imgElement.getAttribute("source");
                    String imgName = source.substring(source.lastIndexOf("/") + 1);
                    // check if image is in same dir or subfolder, since the user has "terrain/terrain_atlas.png"
                    // we will just construct paths
                    String imgPath = basePath + source; // this works if source is relative to tsx
                    
                    // But wait, the source might be "terrain/terrain_atlas.png" and the .tsx is in "view/assets/tiled/".
                    // So basePath + source = "view/assets/tiled/terrain/terrain_atlas.png" - this is correct!
                    // What if source has "../"? We should clean it. Let's just use file name and search inside tiled/ and tiled/terrain/ etc.
                    // simpler: just resolve it if it doesn't have ../
                    if (source.contains("../")) {
                        source = source.substring(source.lastIndexOf("../") + 3);
                    }
                    if (!source.startsWith("view/assets/")) {
                        source = basePath + source;
                    }
                    
                    InputStream imgIs = getStream(source);
                    if (imgIs == null) {
                        // fallback
                        imgIs = getStream(basePath + imgName);
                    }
                    if (imgIs != null) {
                        Image sheet = new Image(imgIs);
                        int cols = Integer.parseInt(tsRoot.getAttribute("columns"));
                        int count = Integer.parseInt(tsRoot.getAttribute("tilecount"));
                        int tWidth = Integer.parseInt(tsRoot.getAttribute("tilewidth"));
                        int tHeight = Integer.parseInt(tsRoot.getAttribute("tileheight"));
                        PixelReader reader = sheet.getPixelReader();
                        for (int t = 0; t < count; t++) {
                            int x = (t % cols) * tWidth;
                            int y = (t / cols) * tHeight;
                            WritableImage tileImg = new WritableImage(reader, x, y, tWidth, tHeight);
                            tiles.put(firstGid + t, tileImg);
                        }
                    }
                } else {
                    // Image collection
                    NodeList tileNodes = tsRoot.getElementsByTagName("tile");
                    for (int j = 0; j < tileNodes.getLength(); j++) {
                        Element tileElement = (Element) tileNodes.item(j);
                        int id = Integer.parseInt(tileElement.getAttribute("id"));
                        NodeList imgList = tileElement.getElementsByTagName("image");
                        if (imgList.getLength() > 0) {
                            Element imgElement = (Element) imgList.item(0);
                            String source = imgElement.getAttribute("source");
                            if (source.contains("../")) source = source.substring(source.lastIndexOf("../") + 3);
                            String imgPath = basePath + source;
                            InputStream imgIs = getStream(imgPath);
                            if (imgIs == null) imgIs = getStream(basePath + source.substring(source.lastIndexOf("/") + 1));
                            if (imgIs != null) {
                                Image img = new Image(imgIs);
                                tiles.put(firstGid + id, img);
                            }
                        }
                    }
                }
            }

            // Parse Layers
            NodeList layerNodes = doc.getElementsByTagName("layer");
            System.out.println("[TiledMapLoader] Số lượng Tile Layer tìm thấy: " + layerNodes.getLength());
            for (int i = 0; i < layerNodes.getLength(); i++) {
                Element layerElement = (Element) layerNodes.item(i);
                Layer layer = new Layer();
                layer.name = layerElement.getAttribute("name");
                layer.data = new int[height][width];
                
                System.out.println("[TiledMapLoader] Đang đọc Layer: " + layer.name);

                Element dataElement = (Element) layerElement.getElementsByTagName("data").item(0);
                String encoding = dataElement.getAttribute("encoding");
                int index = 0;
                
                if ("csv".equals(encoding) || (encoding.isEmpty() && dataElement.getElementsByTagName("tile").getLength() == 0)) {
                    String dataStr = dataElement.getTextContent().trim();
                    String[] ids = dataStr.split(",");
                    for (int r = 0; r < height; r++) {
                        for (int c = 0; c < width; c++) {
                            if (index < ids.length) {
                                String idStr = ids[index].trim();
                                if (!idStr.isEmpty()) {
                                    try {
                                        layer.data[r][c] = Integer.parseInt(idStr);
                                    } catch (NumberFormatException ex) {
                                        System.err.println("[TiledMapLoader] Lỗi parse ID dạng CSV: " + idStr);
                                    }
                                }
                                index++;
                            }
                        }
                    }
                } else if ("base64".equals(encoding)) {
                    System.err.println("[TiledMapLoader] Lỗi: base64 encoding chưa được hỗ trợ. Hãy save map bằng định dạng CSV hoặc XML.");
                } else {
                    // Fallback to XML <tile> parsing
                    NodeList tileList = dataElement.getElementsByTagName("tile");
                    for (int r = 0; r < height; r++) {
                        for (int c = 0; c < width; c++) {
                            if (index < tileList.getLength()) {
                                Element tElement = (Element) tileList.item(index);
                                if (tElement.hasAttribute("gid")) {
                                    try {
                                        layer.data[r][c] = Integer.parseInt(tElement.getAttribute("gid"));
                                    } catch (NumberFormatException ex) {
                                        System.err.println("[TiledMapLoader] Lỗi parse GID dạng XML");
                                    }
                                }
                                index++;
                            }
                        }
                    }
                }
                
                System.out.println("[TiledMapLoader] -> Đọc xong layer " + layer.name + " (" + index + " tiles)");
                layers.add(layer);
            }

            // Parse Objects
            NodeList objectGroups = doc.getElementsByTagName("objectgroup");
            for (int i = 0; i < objectGroups.getLength(); i++) {
                Element ogElement = (Element) objectGroups.item(i);
                NodeList objectNodes = ogElement.getElementsByTagName("object");
                for (int j = 0; j < objectNodes.getLength(); j++) {
                    Element objElement = (Element) objectNodes.item(j);
                    TiledObject obj = new TiledObject();
                    if (objElement.hasAttribute("id")) obj.id = Integer.parseInt(objElement.getAttribute("id"));
                    if (objElement.hasAttribute("name")) obj.name = objElement.getAttribute("name");
                    if (objElement.hasAttribute("type")) obj.type = objElement.getAttribute("type");
                    if (objElement.hasAttribute("gid")) obj.gid = Integer.parseInt(objElement.getAttribute("gid"));
                    if (objElement.hasAttribute("x")) obj.x = Double.parseDouble(objElement.getAttribute("x"));
                    if (objElement.hasAttribute("y")) obj.y = Double.parseDouble(objElement.getAttribute("y"));
                    if (objElement.hasAttribute("width")) obj.width = Double.parseDouble(objElement.getAttribute("width"));
                    if (objElement.hasAttribute("height")) obj.height = Double.parseDouble(objElement.getAttribute("height"));
                    if (objElement.hasAttribute("rotation")) obj.rotation = Double.parseDouble(objElement.getAttribute("rotation"));
                    
                    NodeList props = objElement.getElementsByTagName("property");
                    for (int k = 0; k < props.getLength(); k++) {
                        Element propElement = (Element) props.item(k);
                        obj.properties.put(propElement.getAttribute("name"), propElement.getAttribute("value"));
                    }
                    objects.add(obj);
                }
            }

            System.out.println("[TiledMapLoader] Tổng số object đã load: " + objects.size());
            System.out.println("=========================================");

        } catch (Exception e) {
            System.err.println("[TiledMapLoader] EXCEPTION PHÁT SINH KHI ĐỌC MAP:");
            e.printStackTrace();
        }
    }

    public Image getTileImage(int gid) {
        return tiles.get(gid);
    }
}
