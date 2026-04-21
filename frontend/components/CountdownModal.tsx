import { useEffect, useRef, useState } from 'react';
import { Modal, View, Text, Animated, StyleSheet } from 'react-native';

type Props = {
  visible: boolean;
  onComplete: () => void;
};

export default function CountdownModal({ visible, onComplete }: Props) {
  const [count, setCount] = useState(5);
  const scale = useRef(new Animated.Value(1)).current;
  const onCompleteRef = useRef(onComplete);
  onCompleteRef.current = onComplete;

  useEffect(() => {
    if (!visible) {
      setCount(5);
      return;
    }

    let current = 5;
    setCount(current);

    const id = setInterval(() => {
      current -= 1;
      setCount(current);
      if (current <= 0) {
        clearInterval(id);
        onCompleteRef.current();
      }
    }, 1000);

    return () => clearInterval(id);
  }, [visible]);

  useEffect(() => {
    scale.setValue(1.4);
    Animated.spring(scale, {
      toValue: 1,
      useNativeDriver: true,
      friction: 3,
    }).start();
  }, [count, scale]);

  return (
    <Modal visible={visible} transparent animationType="fade" statusBarTranslucent>
      <View style={styles.overlay}>
        <Text style={styles.label}>Matchup beginning in...</Text>
        <Animated.Text style={[styles.count, { transform: [{ scale }] }]}>
          {count > 0 ? String(count) : ''}
        </Animated.Text>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.93)',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 24,
  },
  label: {
    color: '#ccc',
    fontSize: 20,
    fontWeight: '600',
  },
  count: {
    color: '#FFD700',
    fontSize: 96,
    fontWeight: '900',
  },
});
