// northwindInventory implements inventoryControl interface

/*---------------------------Import statements------------------------------*/
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class northwindInventory implements inventoryControl{
	
	/*---------------------------Private methods-------------------------------*/
	
	/*
	 getUniqueId method:
	* output: int uniqueId 
	* functionality: this method is used to create unique id for purchase orders.
	*/
	private static int getUniqueId()
	{	
		int max =1000;
		int min =100;
	    return (int) ((Math.random() * ((max - min) + 1)) + min);
	}
	
	/*
	 executeUpdateQuery method:
	* input: String query
	* output: int queryresult
	* functionality: this method is used to execute manipulation queries like Update, Insert
	*/
	private static int executeUpdateQuery(String query) {
		Connection connect = null;
		
		// a place to build up an SQL queries
        Statement statement1 = null;
        int resultSet1 = 0;
        try {
        	String user = "pkaur";
            String password = "B00843735";
        	// load and setup connection with the MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
  
            connect = DriverManager.getConnection("jdbc:mysql://db.cs.dal.ca:3306?serverTimezone=UTC&useSSL=false", user, password);

            // issue SQL query to the database
            statement1 = connect.createStatement();
            statement1.executeQuery("use pkaur;");
            resultSet1 = statement1.executeUpdate( query );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return resultSet1;
	}
	
	/*
	 executeSelectQuery method:
	* input: String query
	* output: ResultSet
	* functionality: this method is used to execute Select queries
	*/
	private static ResultSet executeSelectQuery(String query) {
		Connection connect = null;
		// a place to build up an SQL queries
        Statement statement1 = null;
        ResultSet resultSet1 = null;
        try {

        	String user = "pkaur";
            String password = "B00843735";
        	// load and setup connection with the MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
  
            connect = DriverManager.getConnection("jdbc:mysql://db.cs.dal.ca:3306?serverTimezone=UTC&useSSL=false", user, password);

            // issue SQL query to the database
            statement1 = connect.createStatement();
            statement1.executeQuery("use pkaur;");
            resultSet1 = statement1.executeQuery( query );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return resultSet1;
	}

	/*-----------------------------Public methods------------------------------*/
	
	/*
	 Ship_order method:
	  * input: int orderNumber
	  * functionality: This method is called when order is shipped to clients, it updates 
	  * ship date in orders table and also updates products table's column units
	  * in stock.
	 */
	public void Ship_order(int orderNumber) throws OrderException {
		
		// update shipped date for given order id in orders table
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String ShipDate = dateFormat.format(date);
		Map<Integer, Integer> productDetails= new HashMap<Integer, Integer>();
		String q = "UPDATE orders " + 
				"SET ShippedDate = '"+ShipDate+"' WHERE OrderID = "+orderNumber+";";
		String q1 = "Select * from orderdetails where OrderID = "+orderNumber+";";
		String q3 = "Select * from products where ProductID in(";
		executeUpdateQuery(q);
		ResultSet orderDetails = executeSelectQuery(q1);
		try{
			// throw exception if order doesn't exist
			if (!orderDetails.isBeforeFirst() ) {    
			    throw new OrderException("Order ID:"+orderNumber+" does not exist", orderNumber);
			} 
			
			// for each product in the given order update the UnitsInStock
			while(orderDetails.next()) {
				productDetails.put(orderDetails.getInt("ProductID"),
						orderDetails.getInt("Quantity"));
				q3 +=""+orderDetails.getInt("ProductID")+",";
			}
			q3 +="00);";
			ResultSet productInfo = executeSelectQuery(q3);
			while(productInfo.next()) {
				
				int key = productInfo.getInt("ProductID");
				int value = productDetails.get(key);
				int Quantity = productInfo.getInt("UnitsInStock");
				// throw exception if quantity is less than order items
				if ((Quantity-value)<0) {
					throw new OrderException("Quanity is less that the ask in order", orderNumber);
				}
				productDetails.replace(key, Quantity-value);
				String updateQuantity = "Update products set UnitsInStock="+(Quantity-value)+" where "+
				"ProductId="+key+";";
				executeUpdateQuery(updateQuantity);
			}
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
	}

	/*
	 Issue_reorders method:
	  * input: int year, int month, int day
	  * output: number of suppliers to whom order was placed 
	  * functionality: This method is called at the end of the day to check how many
	  * products need to be reordered, places purchase ordes and update stock in products table
	 */
	public int Issue_reorders(int year, int month, int day) {
		
		// get all the products which need to be reordered
		String reorderProdsquery = "Select * from products where"+
				"(UnitsInStock + UnitsOnOrder) <= ReorderLevel and Discontinued = 0;";
		ResultSet reorderProds = executeSelectQuery(reorderProdsquery);
		Map<Integer, Integer> supplierIDMap = new HashMap<Integer, Integer>();
		String orderDate = ""+year+"-"+month+"-"+day;
		int Quantity = 0;
		double unitPrice = 0.0;
		try{
			while(reorderProds.next()) {
				int prodId = reorderProds.getInt("ProductID");
				int supId = reorderProds.getInt("SupplierID");
				
				// get the latest order price for the product
				String priceQuery = "with latest_order as (Select orderID as latestOrderId from orders"+
						" where orderID in(Select distinct orderID from orderdetails where productID="+prodId+") order "+
						"by orderDate desc limit 1)"+
						" Select * from orderdetails where orderId = ( Select latestOrderId from latest_order) "+
						" and productid ="+prodId+";";
				ResultSet priceDetails = executeSelectQuery(priceQuery);
				if (!supplierIDMap.containsKey(supId)) {
					supplierIDMap.put(supId, getUniqueId());
				}
				while(priceDetails.next()) {
					// its 15% more than the original price so get the original price
					unitPrice = 0.85 * priceDetails.getInt("UnitPrice");
					unitPrice = Math.round(unitPrice*100.0)/100.0;
				}
				Quantity = reorderProds.getInt("ReorderLevel");
				// if reorder level is 0 odder 5 units
				if(Quantity == 0) {
					Quantity = 5;
				}
				
				// place purchase orders for the products
				String purchaseOrderEntry = "insert into purchaseorders values("+
						supplierIDMap.get(supId)+","+supId+","+prodId+","+unitPrice+","+Quantity+","+
						0+",'"+orderDate+"',null);";
				executeUpdateQuery(purchaseOrderEntry);
				
				// update units on order in products table
				String updateProdQuery = "Update products set UnitsOnOrder= UnitsOnOrder+"+Quantity+
						" where ProductID="+prodId+";";
				executeUpdateQuery(updateProdQuery);
			}
		} catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
		
		// return number of suppliers
		return supplierIDMap.size();
	}

	/*
	 Receive_order method:
	  * input: int internal_order_reference
	  * functionality: This method is called when order from supplier is received, it updates 
	  * stock details in product table
	 */
	public void Receive_order(int internal_order_reference) throws OrderException {
		
		// update arrival date for purchase orders
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String ArrivalDate = dateFormat.format(date);
		String purchaseQuery = "Update purchaseorders set ArrivalDate='"+ArrivalDate+"'"+
		" where SupplyOrderID="+internal_order_reference+";";
		executeUpdateQuery(purchaseQuery);
		
		// get all products from products table which were part of the purchase order
		String purchaseOrderQuery = "Select * from purchaseorders where SupplyOrderID="+internal_order_reference+";";
		ResultSet purchaseOrder = executeSelectQuery(purchaseOrderQuery);
		try{
			// throw exception if order doesn't exist
			if (!purchaseOrder.isBeforeFirst() ) {    
			    throw new OrderException("Order ID:"+internal_order_reference+" for purchase order does not exist",
			    		internal_order_reference);
			}
			
			// update stock details in products table
			while(purchaseOrder.next()) {
				int prodId = purchaseOrder.getInt("ProductID");
				String updateStock = "update products set UnitsInStock = "+purchaseOrder.getInt("Quantity")+","
				+ "UnitsOnOrder=0 where ProductID="+prodId+";";
				executeUpdateQuery(updateStock);
				
			}
		} catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
	}
	
	public static void main(String[] args) throws OrderException {
		northwindInventory obj = new northwindInventory();
        obj.Ship_order(10248);
        System.out.println("No. of suppliers:"+obj.Issue_reorders(2019, 03, 13));
        obj.Receive_order(125);

	}

}
