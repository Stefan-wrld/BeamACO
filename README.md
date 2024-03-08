# Implementierung des Beam-ACO Algorithmus für die Bachelorarbeit "Optimierung von Touren- und Auftragsplanung für mobile Dienstleister"

---

Dieses Projekt implementiert den Beam-ACO, der von Manuel Lopez-Ibanez und Christian Blum im Jahr 2010 in "Computers & Operations Research" unter dem Titel "Beam-ACO for the travelling salesman problem with time windows" vorgestellt wurde. Zusätzlich enthält es reale Benchmarkdaten, die von N. Ascheuer in seiner Doktorarbeit "Hamiltonian path problems in the on-line optimization of flexible manufacturing systems" aus dem Jahr 1995 an der Technischen Universität Berlin präsentiert wurden.

---

## Durchführung des Codes

Die Klasse `ACO` enthält in Zeile 7 den Parameter `instance`, dem eine spezifische Instanz aus dem Ordner `instances` übergeben wird. Diese Instanz dient als Grundlage für die Berechnung einer optimierten Tour. Bei Ausführung der `main`-methode der `ACO`-Klasse wird die Optimierung gestartet. Die Instanz "src/instances/AFG/rbg233.tw" entspricht zu begin dem Parameter `instance`.