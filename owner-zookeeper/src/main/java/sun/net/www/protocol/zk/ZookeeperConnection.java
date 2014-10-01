package sun.net.www.protocol.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Koray Sariteke
 */
public class ZookeeperConnection extends URLConnection {
    private static final String ZOOKEEPER_HOST = "zookeeper.host";
    private static final String ZOOKEEPER_PORT = "zookeeper.port";
    private static final String ZOOKEEPER_NODE_ROOT = "zookeeper.node.root";

    private String host;
    private Integer port;

    private final String path;
    private final CuratorFramework client;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    protected ZookeeperConnection(URL url) throws MalformedURLException {
        super(url);

        host = System.getProperty(ZOOKEEPER_HOST);
        port = System.getProperty(ZOOKEEPER_PORT) == null ? null : Integer.valueOf(System.getProperty(ZOOKEEPER_PORT));
        String rootNode = System.getProperty(ZOOKEEPER_NODE_ROOT);
        if (null == host || null == port || null == rootNode) {
            throw new MalformedURLException("host - port - zookeeper root node are needed...");
        }

        path = ZKPaths.makePath(rootNode, url.getPath());
        client = CuratorFrameworkFactory.newClient(host + ":" + port, new ExponentialBackoffRetry(1000, 3));
    }

    @Override
    public void connect() {
        client.start();
    }

    private void close() throws IOException {
        client.close();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        this.connect();
        return new ZookeeperStream(this);
    }


    public class ZookeeperStream extends InputStream {
        private final ZookeeperConnection connection;

        public ZookeeperStream(ZookeeperConnection cache) {
            this.connection = cache;
        }

        public Map<String, String> pairs() {
            try {
                Map<String, String> pairsMap = new HashMap<String, String>();

                for (String key : connection.client.getChildren().forPath(path)) {
                    pairsMap.put(key, new String(connection.client.getData().forPath(ZKPaths.makePath(path, key))));
                }

                return pairsMap;
            } catch (Exception exp) {
                throw new RuntimeException(exp);
            }
        }

        @Override
        public void close() throws IOException {
            connection.close();
        }

        @Override
        public int read() throws IOException {
            throw new IOException("not supported operation");
        }

        @Override
        public int read(byte[] b) throws IOException {
            throw new IOException("not supported operation");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException("not supported operation");
        }

        @Override
        public long skip(long n) throws IOException {
            throw new IOException("not supported operation");
        }

        @Override
        public int available() throws IOException {
            throw new IOException("not supported operation");
        }
    }
}