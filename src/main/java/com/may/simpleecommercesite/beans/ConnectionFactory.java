package com.may.simpleecommercesite.beans;




import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class ConnectionFactory implements Serializable {
    @Resource(lookup = "jdbc/pool/test")
   private DataSource dataSource;
    public ConnectionFactory(){}
    @Produces
    public WrappedConnection getConnection() throws SQLException {
        return new WrappedConnection(dataSource.getConnection("root", "Yusuf_2002"));
    }
    public DataSource getDataSource() {
        return dataSource;
    }
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
