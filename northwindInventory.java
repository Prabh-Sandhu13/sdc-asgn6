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
	int uniqueId = 10;

	int getUniqueId()
	{
	    return uniqueId++;
	}
	public static int executeUpdateQuery(String query) {
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
	
	public static ResultSet executeSelectQuery(String query) {
		Connection connect = null;
		// a place to build up an SQL queries
        Statement statement1 = null;
        ResultSet resultSet1 = null;
        try {

        	String user = "pkaur";
            String password = "B00843735";
        	// load and setup connection with the MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
//           connect = DriverManager.getConnection("jdbc:mysql://localhost/?user=root&password=S@ndhu13*");
//            connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/", user, password);
              
//            connect=DriverManager.getConnection(  
//            "jdbc:mysql://localhost:3306/?serverTimezone=UTC&useSSL=false","root","123456");  
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


	public void Ship_order(int orderNumber) throws OrderException {
		// TODO Auto-generated method stub
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String ShipDate = dateFormat.format(date);
		Map<Integer, Integer> productDetails= new HashMap<Integer, Integer>();
		String q = "UPDATE orders " + 
				"SET ShippedDate = '"+ShipDate+"' WHERE OrderID = "+orderNumber+";";
		String q1 = "Select * from orderdetails where OrderID = "+orderNumber+";";
		String q3 = "Select * from products where ProductID in(";
		int updateShipDate = executeUpdateQuery(q);
		ResultSet orderDetails = executeSelectQuery(q1);
		try{
			System.out.println(updateShipDate);
			while(orderDetails.next()) {
				System.out.println(orderDetails.getInt("ProductID"));
				productDetails.put(orderDetails.getInt("ProductID"),
						orderDetails.getInt("Quantity"));
				q3 +=""+orderDetails.getInt("ProductID")+",";
			}
			
			if (!orderDetails.isBeforeFirst() ) {    
			    System.out.println("No data"); 
			    throw new OrderException("Order ID:"+orderNumber+" does not exist", orderNumber);
			} 
			System.out.println(productDetails);
			q3 +="00);";
			ResultSet productInfo = executeSelectQuery(q3);
			while(productInfo.next()) {
				
				int key = productInfo.getInt("ProductID");
				int value = productDetails.get(key);
				int Quantity = productInfo.getInt("UnitsInStock");
				System.out.println(key+":"+Quantity);
				if ((Quantity-value)<0) {
					throw new OrderException("Quanity is less that the ask in order", orderNumber);
				}
				productDetails.replace(key, Quantity-value);
				String updateQuantity = "Update products set UnitsInStock="+(Quantity-value)+" where "+
				"ProductId="+key+";";
				int isQuantityUpdated = executeUpdateQuery(updateQuantity);
				System.out.println(isQuantityUpdated);
			}
			System.out.println(productDetails);
			

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
	}

	@Override
	public int Issue_reorders(int year, int month, int day) {
		String reorderProdsquery = "Select * from products where"+
				"(UnitsInStock + UnitsOnOrder) <= ReorderLevel and Discontinued = 0;";
		ResultSet reorderProds = executeSelectQuery(reorderProdsquery);
		Map<Integer, Integer> supplierIDMap = new HashMap<Integer, Integer>();
		String orderDate = ""+year+"-"+month+"-"+day;
		int SupplyOrderId = 0;
		int SupplierID = 0;
		int ProductID = 0;
		int Quantity = 0;
		int TrackingID = 0;
		double unitPrice = 0.0;
		try{
			while(reorderProds.next()) {
				int prodId = reorderProds.getInt("ProductID");
				int supId = reorderProds.getInt("SupplierID");
				System.out.println(reorderProds.getInt("ProductID"));
				System.out.println("SupplierId:"+reorderProds.getInt("SupplierID"));
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
					System.out.println("UnitPrice:"+priceDetails.getInt("UnitPrice"));
					unitPrice = 0.85 * priceDetails.getInt("UnitPrice");
					unitPrice = Math.round(unitPrice*100.0)/100.0;
					System.out.println("UnitPrice:"+unitPrice);
				}
				Quantity = reorderProds.getInt("ReorderLevel");
				if(Quantity == 0) {
					Quantity = 5;
				}
				String purchaseOrderEntry = "insert into purchaseorders values("+
						supplierIDMap.get(supId)+","+supId+","+prodId+","+unitPrice+","+Quantity+","+
						0+",'"+orderDate+"',null);";
				executeUpdateQuery(purchaseOrderEntry);
				String updateProdQuery = "Update products set UnitsOnOrder= UnitsOnOrder+"+Quantity+
						" where ProductID="+prodId+";";
				executeUpdateQuery(updateProdQuery);
			}
		} catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
		
		return supplierIDMap.size();
	}

	@Override
	public void Receive_order(int internal_order_reference) throws OrderException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String ArrivalDate = dateFormat.format(date);
		String purchaseQuery = "Update purchaseorders set ArrivalDate='"+ArrivalDate+"'"+
		" where SupplyOrderID="+internal_order_reference+";";
		executeUpdateQuery(purchaseQuery);
		String purchaseOrderQuery = "Select * from purchaseorders where SupplyOrderID="+internal_order_reference+";";
		ResultSet purchaseOrder = executeSelectQuery(purchaseOrderQuery);
		try{
			while(purchaseOrder.next()) {
				int prodId = purchaseOrder.getInt("ProductID");
				String updateStock = "update products set UnitsInStock = "+purchaseOrder.getInt("Quantity")+","
				+ "UnitsOnOrder=0 where ProductID="+prodId+";";
				executeUpdateQuery(updateStock);
				
			}
			if (!purchaseOrder.isBeforeFirst() ) {    
			    System.out.println("No data"); 
			    throw new OrderException("Order ID:"+internal_order_reference+" for purchase order does not exist",
			    		internal_order_reference);
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
        obj.Receive_order(10);

	}

}
