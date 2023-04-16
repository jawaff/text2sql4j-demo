import unittest
from parser import generated_select_stmt

class SqlParserTest(unittest.TestCase):
    def test_complete1(self):
        sql = 'concert_singer | select t1.concert_name from concert as t1 join stadium as t2 on t1.stadium_id = t2.stadium_id order by max(capacity)'
        print(sql)
        res = generated_select_stmt.parseString(sql, parseAll=True)
        print(res)

    def test_order_by(self):
        sql = 'concert_singer | select * from table order by max(capacity) desc'
        print(sql)
        res = generated_select_stmt.parseString(sql, parseAll=True)
        print(res)

    def test_where_select(self):
        sql = 'concert_singer | select * from table as t3 where capacity > (select max(capacity) from stadium);'
        print(sql)
        res = generated_select_stmt.parseString(sql, parseAll=True)
        print(res)

    def test_complete2(self):
        sql = 'concert_singer | select t1.concert_name from concert as t1 join stadium as t2 on t1.stadium_id = t2.stadium_id order by max (t2.capacity) desc'
        print(sql)
        res = generated_select_stmt.parseString(sql, parseAll=True)
        print(res)

    def test_complete3(self):
        sql = 'concert_singer | select t1.concert_name from concert as t1 join stadium as t2 on t1.stadium_id = t2.stadium_id order by max(t2.capacity) desc'
        print(sql)
        res = generated_select_stmt.parseString(sql, parseAll=True)
        print(res)

    def test_incomplete_sql(self):
        sql = 'concert_singer | is this invalid?'
        print(sql)
        res = generated_select_stmt.parseString(sql, parseAll=False)
        print(res)

    def test_pieces(self):
        sql = 'concert_singer | select t1.concert_name from concert as t1 join stadium as t2 on t1.stadium_id = t2.stadium_id order by max(t2.capacity) desc'
        keywords = [' select', ' from', ', ', ' and', ' or', ' group', ' order', ' join']
        # TODO Do a find all for all of these keywords. We need to validate prior to each of them.
        #for keyword in keywords:
        #    i = sql.find(keyword)
        #    print('Keyword: ' + keyword)
        #    print('Next Test: ' + sql[0:i])
        #    generated_select_stmt.parseString(sql[0:i], parseAll=True)
