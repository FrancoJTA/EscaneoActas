# EscaneoActas
Este proyecto es una aplicación Android que permite escanear imágenes de actas electorales, segmentar las regiones con dígitos manuscritos, y clasificarlos usando un modelo entrenado de TensorFlow Lite (basado en MNIST o similar).

## 🧠 Tecnologías usadas

- **Kotlin (Android)**
- **OpenCV** para segmentación y procesamiento de imagen
- **TensorFlow Lite** para clasificación de dígitos
- **Canvas + ByteBuffer** para preprocesamiento y visualización
- **Modelo `mnist.tflite`** optimizado para reconocimiento de números del 0 al 9

---

## 📷 Flujo de trabajo del sistema

1. **Captura o selección de imagen**:
   - Se obtiene la imagen desde la galería o cámara y se pasa a la actividad `SegmentacionActivity`.

2. **Preprocesamiento con OpenCV**:
   - Conversión a escala de grises.
   - Desenfoque Gaussiano para reducir ruido.
   - Umbral binario inverso (`THRESH_BINARY_INV`).
   - Apertura morfológica para limpiar residuos.
   - Detección de contornos externos (`findContours`).

3. **Segmentación de regiones de interés (ROIs)**:
   - Cada contorno válido se recorta y se centra en una imagen cuadrada con fondo negro.
   - Se aplica dilatación para engrosar los trazos y mejorar el reconocimiento.

4. **Clasificación con TensorFlow Lite**:
   - Cada ROI se escala a 28x28 px.
   - Se convierte a escala de grises sin invertir (los dígitos son oscuros sobre fondo claro).
   - Se escribe en un `ByteBuffer` y se ejecuta inferencia con el modelo `mnist.tflite`.

5. **Visualización**:
   - Los dígitos reconocidos con más del 50% de confianza se dibujan en la imagen y se muestran en miniaturas en una barra inferior (`roiContainer`).
   - También se puede visualizar cómo ve el modelo los píxeles procesados para debug.