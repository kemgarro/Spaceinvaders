package cr.ac.tec.spaceinvaders.admin;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ventana del admin client: una interfaz Swing que permite enviar comandos
 * {@code ADMIN_CMD} al servidor en tiempo real.
 *
 * <p>Secciones de la UI:</p>
 * <ul>
 *   <li><b>Conexion</b>: host, puerto, id del admin y boton conectar/desconectar.</li>
 *   <li><b>Crear alien</b>: X, Y y tipo (10/20/40 puntos).</li>
 *   <li><b>Spawn OVNI</b>: direccion y puntos base.</li>
 *   <li><b>Velocidad</b>: intervalo en ms (50..2000) del bloque de aliens.</li>
 *   <li><b>Bunkers</b>: porcentaje (0..100) de salud.</li>
 *   <li><b>Log</b>: traza de comandos enviados y respuestas del servidor.</li>
 * </ul>
 *
 * <p>El procesamiento de red corre en {@link AdminConexion} y los callbacks
 * actualizan la UI a traves de {@link SwingUtilities#invokeLater(Runnable)}
 * para respetar el modelo single-threaded de Swing.</p>
 */
public class AdminVentana {

    private static final SimpleDateFormat HORA = new SimpleDateFormat("HH:mm:ss");

    private final JFrame frame;
    private final JTextField campoHost;
    private final JTextField campoPuerto;
    private final JTextField campoAdminId;
    private final JButton botonConectar;
    private final JLabel etiquetaEstado;

    private final JSpinner spinnerX;
    private final JSpinner spinnerY;
    private final JRadioButton radio10;
    private final JRadioButton radio20;
    private final JRadioButton radio40;
    private final JButton botonCrear;

    private final JRadioButton radioIzqDer;
    private final JRadioButton radioDerIzq;
    private final JSpinner spinnerOvniPts;
    private final JButton botonOvni;

    private final JSpinner spinnerVelocidad;
    private final JButton botonVelocidad;

    private final JSpinner spinnerBunkers;
    private final JButton botonBunkers;

    private final JTextArea areaLog;

    private AdminConexion conexion;

    /**
     * Construye la ventana sin mostrarla aun. Llamar a {@link #mostrar()}.
     *
     * @param hostDefault  host por defecto en el campo de conexion.
     * @param puertoDefault puerto por defecto.
     * @param adminIdDefault id por defecto del admin.
     */
    public AdminVentana(String hostDefault, int puertoDefault, String adminIdDefault) {
        frame = new JFrame("Admin — spaCEinvaders");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.setMinimumSize(new Dimension(640, 720));

        // ------- Panel superior: conexion --------
        JPanel panelConexion = new JPanel(new GridLayout(2, 1, 4, 4));
        panelConexion.setBorder(BorderFactory.createTitledBorder("Conexion"));

        JPanel filaCampos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filaCampos.add(new JLabel("Host:"));
        campoHost = new JTextField(hostDefault, 12);
        filaCampos.add(campoHost);
        filaCampos.add(new JLabel("Puerto:"));
        campoPuerto = new JTextField(String.valueOf(puertoDefault), 6);
        filaCampos.add(campoPuerto);
        filaCampos.add(new JLabel("Admin id:"));
        campoAdminId = new JTextField(adminIdDefault, 10);
        filaCampos.add(campoAdminId);
        panelConexion.add(filaCampos);

        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        botonConectar = new JButton("Conectar");
        etiquetaEstado = new JLabel("● Desconectado");
        etiquetaEstado.setFont(etiquetaEstado.getFont().deriveFont(Font.BOLD));
        filaBoton.add(botonConectar);
        filaBoton.add(etiquetaEstado);
        panelConexion.add(filaBoton);

        // ------- Panel central: secciones de comandos --------
        JPanel panelComandos = new JPanel();
        panelComandos.setLayout(new BoxLayout(panelComandos, BoxLayout.Y_AXIS));

        // Crear alien
        JPanel panelCrear = panelConTitulo("Crear alien");
        spinnerX = new JSpinner(new SpinnerNumberModel(400, 0, 2000, 10));
        spinnerY = new JSpinner(new SpinnerNumberModel(200, 0, 2000, 10));
        radio10 = new JRadioButton("10 (Squid)");
        radio20 = new JRadioButton("20 (Crab)");
        radio40 = new JRadioButton("40 (Octopus)", true);
        ButtonGroup grupoPts = new ButtonGroup();
        grupoPts.add(radio10);
        grupoPts.add(radio20);
        grupoPts.add(radio40);
        botonCrear = new JButton("Crear");
        panelCrear.add(new JLabel("X:"));
        panelCrear.add(spinnerX);
        panelCrear.add(new JLabel("Y:"));
        panelCrear.add(spinnerY);
        panelCrear.add(Box.createHorizontalStrut(8));
        panelCrear.add(radio10);
        panelCrear.add(radio20);
        panelCrear.add(radio40);
        panelCrear.add(Box.createHorizontalStrut(8));
        panelCrear.add(botonCrear);
        panelComandos.add(panelCrear);

        // Spawn OVNI
        JPanel panelOvni = panelConTitulo("Spawn OVNI");
        radioIzqDer = new JRadioButton("Izq → Der", true);
        radioDerIzq = new JRadioButton("Der → Izq");
        ButtonGroup grupoDir = new ButtonGroup();
        grupoDir.add(radioIzqDer);
        grupoDir.add(radioDerIzq);
        spinnerOvniPts = new JSpinner(new SpinnerNumberModel(1500, 100, 10000, 100));
        botonOvni = new JButton("Lanzar OVNI");
        panelOvni.add(new JLabel("Direccion:"));
        panelOvni.add(radioIzqDer);
        panelOvni.add(radioDerIzq);
        panelOvni.add(Box.createHorizontalStrut(8));
        panelOvni.add(new JLabel("Puntos base:"));
        panelOvni.add(spinnerOvniPts);
        panelOvni.add(Box.createHorizontalStrut(8));
        panelOvni.add(botonOvni);
        panelComandos.add(panelOvni);

        // Velocidad
        JPanel panelVelocidad = panelConTitulo("Velocidad aliens");
        spinnerVelocidad = new JSpinner(new SpinnerNumberModel(800, 50, 2000, 50));
        botonVelocidad = new JButton("Aplicar");
        panelVelocidad.add(new JLabel("Intervalo (ms):"));
        panelVelocidad.add(spinnerVelocidad);
        panelVelocidad.add(new JLabel("(50=rapidisimo, 800=default, 2000=lento)"));
        panelVelocidad.add(Box.createHorizontalStrut(8));
        panelVelocidad.add(botonVelocidad);
        panelComandos.add(panelVelocidad);

        // Bunkers
        JPanel panelBunkers = panelConTitulo("Salud bunkers");
        spinnerBunkers = new JSpinner(new SpinnerNumberModel(100, 0, 100, 10));
        botonBunkers = new JButton("Aplicar");
        panelBunkers.add(new JLabel("Porcentaje:"));
        panelBunkers.add(spinnerBunkers);
        panelBunkers.add(new JLabel("% (0=destruir, 100=full)"));
        panelBunkers.add(Box.createHorizontalStrut(8));
        panelBunkers.add(botonBunkers);
        panelComandos.add(panelBunkers);

        // ------- Panel inferior: log --------
        areaLog = new JTextArea(14, 60);
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createTitledBorder("Log"));

        // Ensamble final
        JPanel norte = new JPanel(new BorderLayout(4, 4));
        norte.add(panelConexion, BorderLayout.NORTH);
        norte.add(panelComandos, BorderLayout.CENTER);
        frame.add(norte, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);

        // Listeners
        botonConectar.addActionListener(e -> toggleConexion());
        botonCrear.addActionListener(e -> enviarCrearAlien());
        botonOvni.addActionListener(e -> enviarOvni());
        botonVelocidad.addActionListener(e -> enviarVelocidad());
        botonBunkers.addActionListener(e -> enviarBunkers());

        habilitarComandos(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /** Muestra la ventana. Debe llamarse desde el EDT. */
    public void mostrar() {
        frame.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Construccion auxiliar de paneles
    // ------------------------------------------------------------------

    private JPanel panelConTitulo(String titulo) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        TitledBorder borde = BorderFactory.createTitledBorder(titulo);
        p.setBorder(borde);
        return p;
    }

    private void habilitarComandos(boolean activo) {
        botonCrear.setEnabled(activo);
        botonOvni.setEnabled(activo);
        botonVelocidad.setEnabled(activo);
        botonBunkers.setEnabled(activo);
    }

    private void log(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            areaLog.append("[" + HORA.format(new Date()) + "] " + mensaje + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    // ------------------------------------------------------------------
    // Listeners de botones
    // ------------------------------------------------------------------

    private void toggleConexion() {
        if (conexion != null && conexion.estaConectado()) {
            conexion.cerrar();
            conexion = null;
            etiquetaEstado.setText("● Desconectado");
            botonConectar.setText("Conectar");
            habilitarComandos(false);
            return;
        }
        try {
            String host = campoHost.getText().trim();
            int puerto = Integer.parseInt(campoPuerto.getText().trim());
            String adminId = campoAdminId.getText().trim();
            if (adminId.isEmpty()) {
                log("error: admin id vacio");
                return;
            }
            conexion = new AdminConexion(host, puerto, adminId,
                this::onMensajeServidor, this::log);
            conexion.conectar();
            etiquetaEstado.setText("● Conectado a " + host + ":" + puerto);
            botonConectar.setText("Desconectar");
            habilitarComandos(true);
        } catch (Exception ex) {
            log("fallo conexion: " + ex.getMessage());
            conexion = null;
        }
    }

    private void enviarCrearAlien() {
        int x = (int) spinnerX.getValue();
        int y = (int) spinnerY.getValue();
        int pts = radio10.isSelected() ? 10 : (radio20.isSelected() ? 20 : 40);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("x", x);
        payload.put("y", y);
        payload.put("pts", pts);
        conexion.enviarComando("CREATE_ALIEN", payload);
    }

    private void enviarOvni() {
        String dir = radioIzqDer.isSelected() ? "I-D" : "D-I";
        int puntosBase = (int) spinnerOvniPts.getValue();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("direccion", dir);
        payload.put("puntosBase", puntosBase);
        conexion.enviarComando("SPAWN_OVNI", payload);
    }

    private void enviarVelocidad() {
        int ms = (int) spinnerVelocidad.getValue();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("intervaloMs", ms);
        conexion.enviarComando("SET_VELOCIDAD", payload);
    }

    private void enviarBunkers() {
        int pct = (int) spinnerBunkers.getValue();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pct", pct);
        conexion.enviarComando("SET_BUNKERS", payload);
    }

    /**
     * Callback de mensajes del servidor. Filtra los STATE (ruidoso, 20 TPS) y
     * solo muestra EVENT y ERROR en el log.
     */
    private void onMensajeServidor(String json) {
        if (json == null || json.isEmpty()) return;
        if (json.contains("\"type\":\"STATE\"")) {
            // STATE llega muy seguido; lo dejamos fuera del log para no
            // ahogar la consola visual. Si quisieras verlo, comentar este if.
            return;
        }
        log("← " + json);
    }
}
