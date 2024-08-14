package iterators;
import java.lang.*;
import java.util.*;
import javax.sql.*;
import java.sql.*;
import com.mysql.cj.jdbc.Driver;


class DatabaseClient {
	private String dbms_name;
	private String host_name;
	private String port_no;
	private String database_name;
	private String user;
	private String pass;


	public DatabaseClient(String dbms_name, String host_name, String port_no, String database_name, String user, String pass) {
		this.dbms_name = dbms_name;
		this.host_name = host_name;
		this.port_no = port_no;
		this.database_name = database_name;
		this.user = user;
		this.pass = pass;
	}


	/**
	 * query - a public method that will connect to the database and execute the sql query passed in
	 * @param sql_text - the String representing the sql you intend to run
	 * @return - returns nothing (void)
	 */
	public void query(String sql_text) {
		String database_address = "jdbc:" + this.dbms_name + "://" + this.host_name + ":" +
					this.port_no + "/" + this.database_name;
		try (Connection con = DriverManager.getConnection(database_address, this.user, this.pass)) {
			try (Statement stmt = con.createStatement()) {
				stmt.execute(sql_text);
			}
		} catch (SQLException err) {
			err.printStackTrace();
		}
	}


}
