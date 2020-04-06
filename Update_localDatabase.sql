use pkaur;
DROP TABLE IF EXISTS `purchaseorders`;

CREATE TABLE `purchaseorders` (
  `SupplyOrderID` int(11) NOT NULL,
  `SupplierID` int(11) NOT NULL,
  `ProductID` int(11) NOT NULL,
  `UnitPrice` decimal(10,4) NOT NULL DEFAULT '0.0000',
  `Quantity` smallint(2) NOT NULL DEFAULT '1',
  `TrackingID` int(11) NOT NULL,
  `OrderDate` date DEFAULT NULL,
  `ArrivalDate` date DEFAULT NULL,
  PRIMARY KEY (`SupplyOrderID`,`ProductID`,`OrderDate`),
  KEY `FK_purchase_orders_products` (`ProductID`),
  CONSTRAINT `FK_purchase_orders_products` FOREIGN KEY (`ProductID`) REFERENCES `products` (`ProductID`),
  CONSTRAINT `FK_purchase_orders_supplier` FOREIGN KEY (`SupplierID`) REFERENCES `suppliers` (`SupplierID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- setting few product quantities to zero
Update products set UnitsInStock = 0, UnitsOnOrder=0 where ProductID in(1,2,3,4,5);