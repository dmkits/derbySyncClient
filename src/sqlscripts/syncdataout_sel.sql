/*данные первой не отправленной записи из таблицы исходящих данных синхронизации */
select ID, CRDATE, TABLENAME, OTYPE, TABLEKEY1IDNAME, TABLEKEY1IDVAL, TABLEKEY2IDNAME, TABLEKEY2IDVAL
    from APP.SYNCDATAOUT 
    where ID= (select min(ID) from APP.SYNCDATAOUT where STATUS is null)
    order by ID
