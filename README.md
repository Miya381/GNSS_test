# GNSSLoggerR
GNSSLoggerR v0.0.1a

オリジナルのGNSSLoggerにRINEX出力、センサー出力、各種ログ機能追加などの改良を施したバージョンです。
現段階の機能は次の通りです。

コード擬似距離・搬送波位相の計算
気圧・磁気・加速度センサーの値の取得、及びそれらを用いた端末の角度計算
磁気コンパスにおいて地磁気と真北とのズレ（磁気偏角）の計算及びその補正
RINEX ver2.11の出力

ver1.2更新内容一覧  
Pseudorange Smootherの適用範囲をGLONASS,QZSSに拡大.  
NMEAの出力に対応.  
観測時間する時間を秒単位で設定する機能を追加.  
Huawei Honor 8でのバグを修正.
