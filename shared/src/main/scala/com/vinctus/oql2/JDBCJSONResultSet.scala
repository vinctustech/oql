package com.vinctus.oql2

import java.sql.ResultSet

class JDBCJSONResultSet(rs: ResultSet) extends JDBCResultSet(rs) with JSONResultSet
