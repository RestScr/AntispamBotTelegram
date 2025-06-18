<?php
include "config.php";
$err = false;
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
try {
    $db = new mysqli($host, $dbuser, $dbpass, $dbname);
    $db->set_charset("utf8mb4");
    $db->options(MYSQLI_OPT_INT_AND_FLOAT_NATIVE, 1);
} catch (mysqli_sql_exception $e) {
    die("Ошибка подключения: " . $e->getMessage());
}
?>