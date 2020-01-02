# Some handy functions for log handling / debugging

def in_gen_ops(acc_id):
    return in_ops(gen_ops, acc_id)

def out_gen_ops(acc_id):
    return out_ops(gen_ops, acc_id)

def in_ser_ops(acc_id):
    return in_ops(ser_ops, acc_id)

def out_ser_ops(acc_id):
    return out_ops(ser_ops, acc_id)

def in_res_ops(acc_id):
    return in_ops(res_ops, acc_id)

def out_res_ops(acc_id):
    return out_ops(res_ops, acc_id)

def in_ops(ops, acc_id):
    return [op for op in ops if op[1] == acc_id]

def out_ops(ops, acc_id):
    return [op for op in ops if op[0] == acc_id]

def total_amount(ops):
    return sum([op[2] for op in ops])

if __name__ == "__main__":
    print("Hello World!")