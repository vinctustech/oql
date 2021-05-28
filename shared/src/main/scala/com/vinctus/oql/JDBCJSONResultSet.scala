package com.vinctus.oql

import java.sql.ResultSet

class JDBCJSONResultSet(rs: ResultSet) extends JDBCResultSet(rs) with JSONResultSet
