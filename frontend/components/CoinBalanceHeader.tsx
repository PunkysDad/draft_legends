import { View, Text, StyleSheet } from 'react-native';
import { useAuth } from '../context/AuthContext';

export default function CoinBalanceHeader() {
  const { user } = useAuth();
  const balance = user?.coinBalance ?? 0;

  return (
    <View style={styles.container}>
      <Text style={styles.icon}>🪙</Text>
      <Text style={styles.balance}>{balance.toLocaleString()}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 16,
  },
  icon: {
    fontSize: 18,
    marginRight: 4,
  },
  balance: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
