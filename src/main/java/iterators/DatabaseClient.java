package iterators;
import java.lang.*;
import java.util.*;
import javax.sql.*;
import java.sql.*;
import com.mysql.cj.jdbc.Driver;

class DatabaseClient {
	private String host_name;
	private String port_no;
	private String database_name;
	private String user;
	private String pass;

	public DatabaseClient(String host_name, String port_no, String database_name, String user, String pass) {
		this.host_name = host_name;
		this.port_no = port_no;
		this.database_name = database_name;
		this.user = user;
		this.pass = pass;
	}

}
